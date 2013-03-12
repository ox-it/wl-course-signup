package uk.ac.ox.oucs.vle.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import uk.ac.ox.oucs.vle.CourseSignupService;

public class CourseSignupController extends AbstractController {

	private UserDirectoryService userDirectoryService;
	public void setUserService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}

	private CourseSignupService courseSignupService;
	public void setCourseSignupService(CourseSignupService courseSignupService) {
		this.courseSignupService = courseSignupService;
	}

	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			 HttpServletResponse response) throws Exception {

		ModelAndView modelAndView = new ModelAndView(request.getPathInfo().substring(1));
		modelAndView.addObject("externalUser", 
				userDirectoryService.getAnonymousUser().equals(userDirectoryService.getCurrentUser()));

		modelAndView.addObject("isAdministrator", 
				!courseSignupService.getAdministering().isEmpty());

		modelAndView.addObject("isApprover",
				!courseSignupService.getApprovals().isEmpty());
		
		modelAndView.addObject("isPending",
				!courseSignupService.getPendings().isEmpty());

		modelAndView.addObject("skinRepo",
				serverConfigurationService.getString("skin.repo", "/library/skin"));
		
		modelAndView.addObject("skinDefault",
				serverConfigurationService.getString("skin.default", "default"));
		
		return modelAndView;
	}
}
