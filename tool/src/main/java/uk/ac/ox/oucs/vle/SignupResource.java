/*
 * #%L
 * Course Signup Webapp
 * %%
 * Copyright (C) 2010 - 2013 University of Oxford
 * %%
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *             http://opensource.org/licenses/ecl2
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package uk.ac.ox.oucs.vle;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.ContextResolver;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.type.TypeFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.user.cover.UserDirectoryService;

import uk.ac.ox.oucs.vle.CourseSignupService.Status;

import com.sun.jersey.api.view.Viewable;

//@Path("/signup")
@Path("signup{cobomo:(/cobomo)?}")
public class SignupResource {

	
	private CourseSignupService courseService;
	private ObjectMapper objectMapper;
	private ServerConfigurationService serverConfigurationService;

	public SignupResource(@Context ContextResolver<Object> resolver) {
		this.courseService = (CourseSignupService) resolver.getContext(CourseSignupService.class);
		serverConfigurationService = (ServerConfigurationService) resolver.getContext(ServerConfigurationService.class);
		objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
		objectMapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
		objectMapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
	}
	
	@Path("/my")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getMySignups() {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		final List<CourseSignup> signups = courseService.getMySignups(null);
		return new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				objectMapper.typedWriter(TypeFactory.collectionType(List.class, CourseSignup.class)).writeValue(output, signups);
			}
		};
	}
	
	@Path("/my/course/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getMyCourseSignups(@PathParam("id") String courseId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		final List<CourseSignup> signups = courseService.getMySignups(null);
		final List<CourseSignup> courseSignups = new ArrayList<CourseSignup>();
		for(CourseSignup signup: signups) {
			if (courseId.equals(signup.getGroup().getCourseId())) {
				courseSignups.add(signup);
			}
		}
		return new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				objectMapper.typedWriter(TypeFactory.collectionType(List.class, CourseSignup.class)).writeValue(output, courseSignups);
			}
		}; 
	}

	/**
	 * Make a new signup
	 * 
	 * @param courseId
	 * 		the courseId of the signup
	 * @param components
	 * 		the components to sign up to
	 * @param email
	 * 		the email of the supervisor
	 * @param message
	 * 		the reason for the signup
	 * @return CourseSignup
	 * 		the coursesignup object created
	 */
	@Path("/my/new")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response signup(@FormParam("courseId") String courseId, @FormParam("components")Set<String> components, @FormParam("email")String email, @FormParam("message")String message) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		
		try {
			CourseSignup entity = courseService.signup(courseId, components, email, message);
			ResponseBuilder builder = Response.status(Response.Status.CREATED);
			builder.status(201);
			builder.entity(entity);
			return builder.build();
			
		} catch (NotFoundException e) {
			throw new WebAppNotFoundException();
			
		} catch (IllegalStateException e) {
			Map<String, String> myMap = new HashMap<String, String>();
			myMap.put("status", "failed");
			myMap.put("message", e.getMessage());
			throw new WebAppBadRequestException(myMap);
			
		} catch (IllegalArgumentException e) {
			Map<String, String> myMap = new HashMap<String, String>();
			myMap.put("status", "failed");
			myMap.put("message", e.getMessage());
			throw new WebAppBadRequestException(myMap);
		}
	}
	
	@Path("/new")
	@POST
	public Response signup(@FormParam("userId")String userId, @FormParam("courseId") String courseId, @FormParam("components")Set<String> components, @FormParam("supervisorId")String supervisorId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.signup(userId, courseId, components, supervisorId);
		return Response.ok().build();
	}
	
	@Path("/supervisor")
	@POST
	public Response signup(@FormParam("signupId")String signupId, @FormParam("supervisorId")String supervisorId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.setSupervisor(signupId, supervisorId);
		return Response.ok().build();
	}
	
	@Path("/{id}")
	@GET
	@Produces("application/json")
	public Response getSignup(@PathParam("id") final String signupId) throws JsonGenerationException, JsonMappingException, IOException {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		CourseSignup signup = courseService.getCourseSignup(signupId);
		if (signup == null) {
			return Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
		}
		return Response.ok(objectMapper.writeValueAsString(signup)).build();
	}
	
	@Path("/{id}")
	@POST // PUT Doesn't seem to make it through the portal :-(
	public void updateSignup(@PathParam("id") final String signupId, @FormParam("status") final Status status){
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.setSignupStatus(signupId, status);
	}

	@Path("{id}/accept")
	@POST
	public Response accept(@PathParam("id") final String signupId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.accept(signupId);
		return Response.ok().build();
	}

	@Path("{id}/reject")
	@POST
	public Response reject(@PathParam("id") final String signupId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.reject(signupId);
		return Response.ok().build();
	}
 
	@Path("{id}/withdraw")
	@POST
	public Response withdraw(@PathParam("id") final String signupId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.withdraw(signupId);
		return Response.ok().build();
	}
	
	@Path("{id}/waiting")
	@POST
	public Response waiting(@PathParam("id") final String signupId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.waiting(signupId);
		return Response.ok().build();
	}

	@Path("{id}/approve")
	@POST
	public Response approve(@PathParam("id") final String signupId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.approve(signupId);
		return Response.ok().build();
	}
	
	@Path("{id}/confirm")
	@POST
	public Response confirm(@PathParam("id") final String signupId) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		courseService.confirm(signupId);
		return Response.ok().build();
	}
	
	@Path("/course/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getCourseSignups(@PathParam("id") final String courseId, @QueryParam("status") final Status status) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		// All the pending 
		return new StreamingOutput() {
			
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				Set<Status> statuses = null;
				if (null != status) {
					statuses = Collections.singleton(status);
				}
				List<CourseSignup> signups = courseService.getCourseSignups(courseId, statuses);
				objectMapper.typedWriter(TypeFactory.collectionType(List.class, CourseSignup.class)).writeValue(output, signups);
			}
		};
	}
	
	@Path("/count/course/signups/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCountCourseSignup(@PathParam("id") final String courseId, @QueryParam("status") final Status status) throws JsonGenerationException, JsonMappingException, IOException {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		// All the pending 
		Set<Status> statuses = null;
		if (null != status) {
			statuses = Collections.singleton(status);
		}
		Integer signups = courseService.getCountCourseSignups(courseId, statuses);
		return Response.ok(objectMapper.writeValueAsString(signups)).build();
	}
	
	@Path("/component/{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getComponentSignups(@PathParam("id") final String componentId, @QueryParam("status") final Status status) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				Set<Status> statuses = null;
				if (null != status) {
					statuses = Collections.singleton(status);
				}
				List<CourseSignup> signups = courseService.getComponentSignups(componentId, statuses);
				objectMapper.typedWriter(TypeFactory.collectionType(List.class, CourseSignup.class)).writeValue(output, signups);
			}
			
		};
	}
	
	@Path("/component/{id}.csv")
	@GET
	@Produces("text/comma-separated-values")
	public StreamingOutput getComponentSignupsCSV(@PathParam("id") final String componentId, @Context final HttpServletResponse response) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				CourseComponent component = courseService.getCourseComponent(componentId);
				List<CourseSignup> signups = courseService.getComponentSignups(componentId, null);
				response.addHeader("Content-disposition", "attachment; filename="+getFileName(component)+".csv"); // Force a download
				Writer writer = new OutputStreamWriter(output);
				CSVWriter csvWriter = new CSVWriter(writer);
				csvWriter.writeln(new String[]{
						component.getTitle(), startsText(component)});
				csvWriter.writeln(new String[]{
						"Surname", "Forname", "Email", "SES Status",
						"Year of Study", "Degree Programme", "Affiliation"});
				for(CourseSignup signup : signups) {
					Person user = signup.getUser();
					if (null == user) {
						continue;
					}
					csvWriter.writeln(new String[]{
							user.getLastName(), user.getFirstName(), user.getEmail(), signup.getStatus().toString(),
							user.getYearOfStudy(), user.getDegreeProgram(), buildString(user.getUnits())});
				}
				writer.flush();
			}
			
		};
	}
	
	@Path("/component/{id}.pdf")
	@GET
	@Produces("application/pdf")
	public StreamingOutput getComponentSignupsPDF(@PathParam("id") final String componentId, @Context final HttpServletResponse response) {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				
				System.out.println("/rest/signup/component/"+componentId+"/pdf");
				
				CourseComponent courseComponent = courseService.getCourseComponent(componentId);
				Collection<CourseGroup> courseGroups = courseService.getCourseGroupsByComponent(componentId);
				
				List<CourseSignup> signups = courseService.getComponentSignups(
						componentId, Collections.singleton(Status.CONFIRMED));
				
				response.addHeader("Content-disposition", "attachment; filename="+componentId+".pdf"); // Force a download
				PDFWriter pdfWriter = new PDFWriter(output);
				pdfWriter.writeHead(courseGroups, courseComponent);
				pdfWriter.writeTableHead();
				
				if (!signups.isEmpty()) {
				
					List<Person> persons = new ArrayList<Person>();
					for (CourseSignup signup : signups) {
						if (null != signup.getUser()) {
							persons.add(signup.getUser());
						}
					}
					Collections.sort(persons, new Comparator<Person>() {
						public int compare(Person p1,Person p2) {
							return p1.getLastName().compareTo(p2.getLastName());
						}
					});
				
					pdfWriter.writeTableBody(persons);
					
				}
				pdfWriter.writeTableFoot();
				pdfWriter.close();
			}
		};
	}
	
	@Path("/component/{year}/{id}.xml")
	@GET
	@Produces(MediaType.TEXT_XML)
	
	public StreamingOutput syncComponent(@PathParam("id") final String componentId, 
										 @QueryParam("status") final Status status, 
										 @PathParam("year") final int year) {
		
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
			WebApplicationException {
				
				Collection<CourseComponent> courseComponents = new ArrayList<CourseComponent>();
				
				if ("all".equals(componentId)) {
					courseComponents = courseService.getAllComponents();
				} else {
					courseComponents.add(courseService.getCourseComponent(componentId));
				}
				
				Set<Status> statuses = null;
				if (null != status) {
					statuses = Collections.singleton(status);
				}
				
				AttendanceWriter attendance = new AttendanceWriter(output);
				
				for (CourseComponent courseComponent : courseComponents) {
				
					try {
						List<CourseSignup> signups = courseService.getComponentSignups(
								courseComponent.getPresentationId(), statuses, year);
				
						Collections.sort(signups, new Comparator<CourseSignup>() {
							public int compare(CourseSignup s1,CourseSignup s2) {
								Person p1 = s1.getUser();
								Person p2 = s2.getUser();
								
								int ret = s1.getGroup().getCourseId().compareTo(s2.getGroup().getCourseId());
								
								// this line is giving a NullPointerException
								//return ret == 0 ? p1.getLastName().compareTo(p2.getLastName()) : ret;
								if (ret != 0) {
									return ret;
								}
								
								if (p1 == null) {
							        return (p2 == null) ? 0 : -1;
							    }
							    if (p2 == null) {
							        return 1;
							    }
							    
								if (p1.getLastName() == null) {
							        return (p2.getLastName() == null) ? 0 : -1;
							    }
							    if (p2.getLastName() == null) {
							        return 1;
							    }
							    
							    return p1.getLastName().compareTo(p2.getLastName());
							}
						});
					
						attendance.writeTeachingInstance(courseComponent, signups);
				
					} catch (NotFoundException e) {
						throw new WebApplicationException(Response.Status.NOT_FOUND);
					}
				}
				attendance.close();
			}
		};
	}
	
	@Path("/attendance")
	@GET
	@Produces(MediaType.TEXT_XML)
	public StreamingOutput sync() {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {
			public void write(OutputStream output) throws IOException,
			WebApplicationException {
				
				AttendanceWriter attendance = new AttendanceWriter(output);
				Collection<CourseComponent> courseComponents = courseService.getAllComponents();
				for (CourseComponent courseComponent : courseComponents) {
				
					List<CourseSignup> signups = courseService.getComponentSignups(
							courseComponent.getPresentationId(), Collections.singleton(Status.CONFIRMED));
					
					attendance.writeTeachingInstance(courseComponent, signups);
				}
				attendance.close();
			}
		};
	}
	
	@Path("/pending")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getPendingSignups() {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				List<CourseSignup> signups = courseService.getPendings();
				objectMapper.typedWriter(TypeFactory.collectionType(List.class, CourseSignup.class)).writeValue(output, signups);
			}
			
		}; 
	}
	
	@Path("/approve")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getApproveSignups() {
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				List<CourseSignup> signups = courseService.getApprovals();
				objectMapper.typedWriter(TypeFactory.collectionType(List.class, CourseSignup.class)).writeValue(output, signups);
			}
			
		}; 
	}
	
	@Path("/previous")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public StreamingOutput getPreviousSignups(@QueryParam("userid") final String userId,
											  @QueryParam("componentid") final String componentId,
											  @QueryParam("groupid") final String groupId) {
		
		if (UserDirectoryService.getAnonymousUser().equals(UserDirectoryService.getCurrentUser())) {
			throw new WebAppForbiddenException();
		}
		return new StreamingOutput() {

			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				List<String> componentIds = Arrays.asList(componentId.split(","));
				Set<CourseSignup> signups = new HashSet<CourseSignup>();
				for (CourseSignup signup : courseService.getUserComponentSignups(userId, null)) {
					if (signup.getGroup().getCourseId().equals(groupId)) {
						for (CourseComponent component : signup.getComponents()) {
							if (!componentIds.contains(component.getPresentationId())) {
								signups.add(signup);
							}
						}
					
					}
				}
				objectMapper.typedWriter(TypeFactory.collectionType(Set.class, CourseSignup.class)).writeValue(output, signups);
			}
			
		}; 
	}
	
	@Path("/advance/{id}")
	@GET
	@Produces("text/html")
	public Response advanceGet(@PathParam("id") final String encoded) {
		
		String[] params = courseService.getCourseSignupFromEncrypted(encoded);
		for (int i=0; i<params.length; i++) {
			System.out.println("decoded parameter ["+params[i]+"]");
		}
		CourseSignup signup = courseService.getCourseSignupAnyway(params[0]);
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("signup", signup);
		model.put("encoded", encoded);
		model.put("status", params[1]);
		
		model.put("skinRepo",
				serverConfigurationService.getString("skin.repo", "/library/skin"));
		
		model.put("skinDefault",
				serverConfigurationService.getString("skin.default", "default"));
		
		return Response.ok(new Viewable("/static/advance", model)).build();
	}
	
	@Path("/advance/{id}")
	@POST
	@Produces("text/html")
	public Response advancePost(@PathParam("id") final String encoded, 
								@FormParam("formStatus") final String formStatus) {
		
		if (null == encoded) {
			return Response.noContent().build();
		}
		String[] params = courseService.getCourseSignupFromEncrypted(encoded);
		
		String signupId = params[0];
		//String status = params[1];
		String placementId = params[2];
		
		CourseSignup signup = courseService.getCourseSignupAnyway(signupId);
		if (null == signup) {
			return Response.noContent().build();
		}
		
		switch (getIndex(new String[]{"accept", "approve", "confirm", "reject"}, formStatus.toLowerCase())) {
		
			case 0: 
				courseService.accept(signupId, true, placementId);
				break;
			case 1: 
				courseService.approve(signupId, true, placementId);
				break;
			case 2: 
				courseService.confirm(signupId, true, placementId);
				break;
			case 3: 
				courseService.reject(signupId, true, placementId);
				break;
			default:
				return Response.noContent().build();
		}
		
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("signup", signup);
		 
		model.put("skinRepo",
				serverConfigurationService.getString("skin.repo", "/library/skin"));
		
		model.put("skinDefault",
				serverConfigurationService.getString("skin.default", "default"));
		
		return Response.ok(new Viewable("/static/ok", model)).build();
	}
	
	protected int getIndex(String[] array, String value){
		for(int i=0; i<array.length; i++){
			if(array[i].equals(value)){
				return i;
			}
		}
		return -1;
	} 
	
	private String buildString(Collection<String> collection) {
		StringBuilder sb = new StringBuilder();
		if (!collection.isEmpty()) {
			for(String s: collection) {
				sb.append(s).append('/');
			}
		}
		return sb.toString();
	}
	
	private String startsText(CourseComponent component) {
		if (null != component.getStartsText() && 
				 !component.getStartsText().isEmpty()) {
			return component.getStartsText();
		}
		if (null != component.getStarts()) {
			return new SimpleDateFormat("EEE d MMM yyyy").format(component.getStarts());
		}
		return "";
	}
	
	private String getFileName(CourseComponent component) {
		StringBuilder sb = new StringBuilder();
		sb.append(component.getPresentationId().replaceAll(" ", "_"));
		if (null != component.getWhen()) {
			sb.append("_");
			sb.append(component.getWhen().replaceAll(" ", "_"));
		}
		return sb.toString();
	}

}
