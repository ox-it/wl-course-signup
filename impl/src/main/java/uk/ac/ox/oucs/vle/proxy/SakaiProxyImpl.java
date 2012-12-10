package uk.ac.ox.oucs.vle.proxy;

import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.antivirus.api.VirusFoundException;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.portal.api.PortalService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import uk.ac.ox.oucs.vle.AdditionalUserDetails;
import uk.ac.ox.oucs.vle.SakaiProxy;
import uk.ac.ox.oucs.vle.UserProxy;

/**
 * This is the actual Sakai proxy which talks to the Sakai services.
 * @author buckett
 *
 */
public class SakaiProxyImpl implements SakaiProxy {

	private final static Log log = LogFactory.getLog(SakaiProxyImpl.class);
	
	private String fromAddress;
	
	/**
	 * 
	 */
	private UserDirectoryService userService;
	public void setUserService(UserDirectoryService userService) {
		this.userService = userService;
	}

	/**
	 * 
	 */
	private EmailService emailService;
	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}

	/**
	 * 
	 */
	private EventTrackingService eventService;
	public void setEventService(EventTrackingService eventService) {
		this.eventService = eventService;
	}

	/**
	 * 
	 */
	private ToolManager toolManager;
	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

	/**
	 * 
	 */
	private SiteService siteService;
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	/**
	 * 
	 */
	private PortalService portalService;
	public void setPortalService(PortalService portalService) {
		this.portalService = portalService;
	}
	
	/**
	 * 
	 */
	private AdditionalUserDetails additionalUserDetails;
	public void setAdditionalUserDetails(AdditionalUserDetails additionalUserDetails) {
		this.additionalUserDetails = additionalUserDetails;
	}
	
	/**
	 * 
	 */
	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}
	
	/**
	 * 
	 */
	private ContentHostingService contentHostingService;
	public void setContentHostingService(ContentHostingService contentHostingService) {
		this.contentHostingService = contentHostingService;
	}
	
	/**
	 * 
	 */
	private SessionManager sessionManager;
	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public void init() {
		if (fromAddress == null) {
			fromAddress = serverConfigurationService.getString("course-signup.from", null);
		}
	}
	
	public UserProxy getCurrentUser() {
		User sakaiUser = userService.getCurrentUser();
		UserProxy user = wrapUserProxy(sakaiUser);
		return user;
	}

	public UserProxy findUserById(String id) {
		try {
			return wrapUserProxy(userService.getUser(id));
		} catch (UserNotDefinedException unde) {
			return null;
		}
	}
	
	public UserProxy findStudentById(String id) {
		try {
			return wrapStudentProxy(userService.getUser(id));
		} catch (UserNotDefinedException unde) {
			return null;
		}
	}

	public UserProxy findUserByEmail(String email) {
		Collection<User> users = userService.findUsersByEmail(email);
		if (users.size() == 0) {
			return null;
		} else {
			if (users.size() > 1) {
				log.warn("More than one user found with email: "+ email);
			}
			return wrapUserProxy(users.iterator().next());
		}
		
	}

	public UserProxy findUserByEid(String eid) {
		try {
			return wrapUserProxy(userService.getUserByAid(eid));
		} catch (UserNotDefinedException unde) {
			return null;
		}
	}

	public void sendEmail(String to, String subject, String body) {
		String from = fromAddress;
		if (from == null) {
			from = getCurrentUser().getEmail();
		}
		emailService.send(
				from, // from address
				to, // to address
				subject, // subject
				body, // message body
				null, // header to string
				null, // Reply to string
				null // Additional headers
		);
	}
	
	public void logEvent(String resourceId, String eventType, String placementId) {
		Placement placement = getPlacement(placementId);
		String context = placement.getContext();
		String resource = "/coursesignup/group/"+ resourceId;
		Event event = eventService.newEvent(eventType, resource, context, false, NotificationService.NOTI_OPTIONAL);
		eventService.post(event);
	}

	/**
	 * Just get the current placement.
	 * @return The current placement.
	 * @throws RunTimeException If there isn't a current placement, this happens
	 * when a request comes through that isn't processed by the portal.
	 */

	public Placement getPlacement(String placementId) {
		Placement placement = null;
		if (null == placementId) {
			placement = toolManager.getCurrentPlacement();
		} else {
			placement = siteService.findTool(placementId);
		}
		if (placement == null) {
			try {
				String defaultSiteId = getSiteId();
				if (null == defaultSiteId) {
					throw new RuntimeException("No default tool placement set.");
				}
				Site site = siteService.getSite(defaultSiteId);
				placement = site.getToolForCommonId("course.signup");
				
			} catch(Exception e) {
				throw new RuntimeException("No current tool placement set.");
			}
		}
		
		if (placement == null) {
			throw new RuntimeException("No current tool placement set.");
		}
		return placement;
	}
	
	protected String getSiteId() {
		if (null != serverConfigurationService) {
			return serverConfigurationService.getString("ses.default.siteId", "d0c31496-d5b9-41fd-9ea9-349a7ac3a01a");
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private UserProxy wrapUserProxy(User sakaiUser) {
		if(sakaiUser == null) {
			return null;
		}
		
		List<String> units = sakaiUser.getProperties().getPropertyList("units");
		return new UserProxy(sakaiUser.getId(), sakaiUser.getEid(), 
				sakaiUser.getFirstName(), sakaiUser.getLastName(), sakaiUser.getDisplayName(), 
				sakaiUser.getEmail(),
				sakaiUser.getDisplayId(),
				sakaiUser.getProperties().getProperty("oakOSSID"), 
				sakaiUser.getProperties().getProperty("yearOfStudy"), 
				sakaiUser.getProperties().getProperty("oakStatus"),
				sakaiUser.getProperties().getProperty("primaryOrgUnit"),
				null,
				(units == null)?Collections.EMPTY_LIST:units);
	}
	
	private UserProxy wrapStudentProxy(User sakaiUser) {
		if(sakaiUser == null) {
			return null;
		}
		
		List<String> units = sakaiUser.getProperties().getPropertyList("units");
		return new UserProxy(sakaiUser.getId(), sakaiUser.getEid(), 
				sakaiUser.getFirstName(), sakaiUser.getLastName(), sakaiUser.getDisplayName(), 
				sakaiUser.getEmail(),
				sakaiUser.getDisplayId(),
				sakaiUser.getProperties().getProperty("oakOSSID"), 
				sakaiUser.getProperties().getProperty("yearOfStudy"), 
				sakaiUser.getProperties().getProperty("oakStatus"),
				sakaiUser.getProperties().getProperty("primaryOrgUnit"),
				additionalUserDetails.getDegreeProgram(sakaiUser.getEid()),
				(units == null)?Collections.EMPTY_LIST:units);
	}

	public String getAdminUrl() {
		return getUrl("/static/admin.jsp");
	}
	
	public String getConfirmUrl(String signupId) {
		return getConfirmUrl(signupId, null);
	}
	
	public String getConfirmUrl(String signupId, String placementId) {
		if (null == signupId) {
			return getUrl("/static/pending.jsp", placementId);
		}
		return getUrl("/static/pending.jsp#"+ signupId, placementId);
	}
	
	public String getDirectUrl(String courseId) {
		return getUrl("/static/index.jsp?openCourse="+ courseId);
	}
	
	public String getApproveUrl(String signupId) {
		return getApproveUrl(signupId, null);
	}
	
	public String getApproveUrl(String signupId, String placementId) {
		if (null == signupId) {
			return getUrl("/static/approve.jsp", placementId);
		}
		return getUrl("/static/approve.jsp#"+ signupId, placementId);
	}
	
	public String getAdvanceUrl(String signupId, String status, String placementId) {
		String urlSafe = encode(signupId+"$"+status+"$"+getPlacement(placementId).getId());
		return serverConfigurationService.getServerUrl() +
			"/course-signup/rest/signup/advance/"+urlSafe;
			
	}
	
	public String encode(String uncoded) {
		byte[] encrypted = encrypt(uncoded);
		String base64String = new String(Base64.encodeBase64(encrypted));	
		return base64String.replace('+','-').replace('/','_');
	}
	
	public String uncode(String encoded) {
		String base64String = encoded.replace('-','+').replace('_','/');
		byte[] encrypted = Base64.decodeBase64(base64String.getBytes());
		return decrypt(encrypted);
	}

	public String getMyUrl() {
		return getMyUrl(null);
	}
	
	public String getMyUrl(String placementId) {
		return getUrl("/static/my.jsp", placementId);
	}

	private String getUrl(String toolState) {
		return getUrl(toolState, null);
	}
	private String getUrl(String toolState, String placementId) {
		Placement currentPlacement = getPlacement(placementId);
		//String siteId = currentPlacement.getContext();
		ToolConfiguration toolConfiguration = siteService.findTool(currentPlacement.getId());
		String pageUrl = toolConfiguration.getContainingPage().getUrl();
		Map<String, String[]> encodedToolState = portalService.encodeToolState(currentPlacement.getId(), toolState);
		StringBuilder params = new StringBuilder();
		for (Entry<String, String[]> entry : encodedToolState.entrySet()) {
			for(String value: entry.getValue()) {
				params.append("&");
				params.append(entry.getKey());
				params.append("=");
				params.append(URLEncoder.encode(value));
			}
		}
		if (params.length() > 0) {
			pageUrl += "?"+ params.substring(1); // Trim the leading &
		}
		return pageUrl;
	}

	protected String getSecretKey() {
		return serverConfigurationService.getString("aes.secret.key", "se1?r2eFM8rC5u2K");
	}
	
	protected byte[] encrypt(String string) {
		SecretKeySpec skeySpec = new SecretKeySpec(getSecretKey().getBytes(), "AES");
		try {
			// Instantiate the cipher
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			byte[] bytes = cipher.doFinal(string.getBytes());
			return bytes;
		
		} catch (Exception e) {
			System.out.println("encrypt Exception ["+e.getLocalizedMessage()+"]"); 
		}
		return null;
	}
	
	protected String decrypt(byte[] bytes) {
		SecretKeySpec skeySpec = new SecretKeySpec(getSecretKey().getBytes(), "AES");
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
			byte[] original = cipher.doFinal(bytes);
			return new String(original);
		
		} catch (Exception e) {	
			System.out.println("decrypt Exception ["+e.getLocalizedMessage()+"]"); 
		}	
		return null;
	}

	public Integer getConfigParam(String param, int dflt) {
		return Integer.parseInt(serverConfigurationService.getString(param, new Integer(dflt).toString()));
	}

	public String getConfigParam(String param, String dflt) {
		return serverConfigurationService.getString(param, dflt);
	}
	
	public void writeLog(String contentId, String contentDisplayName, byte[] bytes) 
			throws VirusFoundException, OverQuotaException, ServerOverloadException, PermissionException, TypeException, InUseException {
		
		switchUser();
		ContentResourceEdit cre = null;
		String siteId = getConfigParam("course-signup.site-id", "course-signup");
		String jsonResourceEId = contentHostingService.getSiteCollection(siteId)+ contentId;

		try {
			// editResource() doesn't throw IdUnusedExcpetion but PermissionException
			// when the resource is missing so we first just tco to find it.
			contentHostingService.getResource(jsonResourceEId);
			cre = contentHostingService.editResource(jsonResourceEId);
	
		} catch (IdUnusedException e) {
			try {
				cre = contentHostingService.addResource(jsonResourceEId);
				ResourceProperties props = cre.getPropertiesEdit();
				props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, contentDisplayName);
				cre.setContentType("text/html");
			} catch (Exception e1) {
				log.warn("Failed to create the import log file.", e1);
			}
		}

		cre.setContent(bytes);
		// Don't notify anyone about this resource.
		contentHostingService.commitResource(cre, NotificationService.NOTI_NONE);
	}
	
	/**
	 * This sets up the user for the current request.
	 */
	
	private void switchUser() {
		if (null != sessionManager) {
			org.sakaiproject.tool.api.Session session = sessionManager.getCurrentSession();
			session.setUserEid("admin");
			session.setUserId("admin");
		}
	}
}
