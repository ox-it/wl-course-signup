package uk.ac.ox.oucs.vle;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import uk.ac.ox.oucs.vle.CourseSignupService.Status;

public class CourseSignupImpl implements CourseSignup, java.io.Serializable {

	private CourseSignupDAO dao;
	private CourseSignupServiceImpl service;
	
	public CourseSignupImpl(CourseSignupDAO dao, CourseSignupServiceImpl service) {
		this.dao = dao;
		this.service = service;
	}
	
	public String getId() {
		return dao.getId();
	}

	public Person getUser() {
		UserProxy user = service.loadStudent(dao.getUserId());
		Person person = null;
		String departmentName = null;
		if (user != null) {
			if (null != user.getPrimaryOrgUnit()) {
				Department department = service.findPracDepartment(user.getPrimaryOrgUnit());
				if (null != department) {
					departmentName = department.getName();
				}
			}
			person = new PersonImpl(user.getId(), 
					user.getFirstName(), user.getLastName(), user.getDisplayName(), 
					user.getEmail(), user.getUnits(), user.getWebauthId(), user.getOssId(), 
					user.getYearOfStudy(), user.getDegreeProgram(), departmentName, user.getType());
		}
		return person;
	}

	public Person getSupervisor() {
		String supervisorId = dao.getSupervisorId();
		if (supervisorId == null) {
			return null;
		}
		UserProxy user = service.loadUser(dao.getSupervisorId());
		Person person = null;
		if (user != null) {
			person = new PersonImpl(user.getId(), 
					user.getFirstName(), user.getLastName(), user.getDisplayName(), 
					user.getEmail(), Collections.<String>emptyList(), 
					user.getWebauthId(), user.getOssId(), null, null, null,
					user.getType());
		}
		return person;	}

	public String getNotes() {
		return dao.getMessage();
	}

	public Status getStatus() {
		return dao.getStatus();
	}

	public Set<CourseComponent> getComponents() {
		Set<CourseComponentDAO> componentDaos = dao.getComponents();
		Set<CourseComponent> components = new HashSet<CourseComponent>(componentDaos.size());
		for(CourseComponentDAO componentDao: componentDaos) {
			components.add(new CourseComponentImpl(componentDao, service));
		}
		return components;
	}

	public CourseGroup getGroup() {
		return new CourseGroupImpl(dao.getGroup(), service);
	}

	public Date getCreated() {
		return dao.getCreated();
	}
	
	public String getDepartment() {
		return dao.getDepartment();
	}

}
