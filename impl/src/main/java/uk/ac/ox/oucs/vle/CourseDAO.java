package uk.ac.ox.oucs.vle;

/*
 * #%L
 * Course Signup Implementation
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import uk.ac.ox.oucs.vle.CourseSignupService.Range;
import uk.ac.ox.oucs.vle.CourseSignupService.Status;

public interface CourseDAO {

	CourseGroupDAO findCourseGroupById(String courseId, Range range, Date now);
	
	CourseGroupDAO findCourseGroupById(String courseId);
	
	CourseGroupDAO findAvailableCourseGroupById(String courseId);
	
	List<CourseGroupDAO> findCourseGroupByDept(String dept, Range range, Date now, boolean external);
	
	List<CourseGroupDAO> findCourseGroupBySubUnit(String subunit, Range range, Date now, boolean external);
	
	List<CourseGroupDAO> findCourseGroupByComponent(String componentId);
	
	List<CourseComponentDAO> findCourseGroupsByCalendar(boolean external, String providerId);
	
	List<CourseComponentDAO> findCourseGroupsByNoDates(boolean external, String providerId);
	
	List<Object[]> findSubUnitByDept(String dept);

	List<CourseComponentDAO> findOpenComponents(String id, Date at);
	
	List<CourseComponentDAO> findAllComponents();

	CourseGroupDAO findUpcomingComponents(String courseId, Date available);
	
	CourseComponentDAO findCourseComponent(String id);
	
	CourseComponentDAO newCourseComponent(String id);

	CourseSignupDAO newSignup(String userId, String supervisorId);

	String save(CourseSignupDAO signupDao);

	String save(CourseComponentDAO componentDao);

	CourseSignupDAO findSignupById(String signupId);
	
	CourseSignupDAO findSignupByEncryptId(String signupId);

	List<CourseSignupDAO> findSignupForUser(String userId, Set<Status> statuses);

	CourseGroupDAO newCourseGroup(String id, String title, String dept, String subunit);

	void save(CourseGroupDAO groupDao);

	List<CourseGroupDAO> findAdminCourseGroups(String userId);

	List<CourseSignupDAO> findSignupByCourse(String userId, String courseId, Set<Status> statuses);
	
	Integer countSignupByCourse(String courseId, Set<Status> statuses);

	List<CourseGroupDAO> findCourseGroupByWords(String[] words, Range range, Date date, boolean external);

	List<CourseSignupDAO> findSignupByComponent(String componentId, Set<Status> statuses);

	List<CourseSignupDAO> findSignupPending(String currentUser);
	
	List<CourseSignupDAO> findSignupApproval(String currentUser);
	
	List<CourseSignupDAO> findSignupStillPendingOrAccepted(final Integer period);
	
	List<CourseDepartmentDAO> findApproverDepartments(String currentUserId);
	
	List<Object[]> findDepartmentApprovers(final String department);
	
	List<CourseDepartmentDAO> findAllDepartments();
	
	CourseDepartmentDAO findDepartmentByCode(String code);
	
	CourseDepartmentDAO findDepartmentByPrimaryOrgUnit(String primaryorgUnit);
	
	void save(CourseDepartmentDAO departmentDao);
	
	CourseSubunitDAO findSubunitByCode(String code);
	
	void save(CourseSubunitDAO subunitDao);
	
	CourseOucsDepartmentDAO findOucsDeptByCode(String code);
	
	void save(CourseOucsDepartmentDAO oucsDao);

	void remove(CourseSignupDAO existingSignup);
	
	CourseUserPlacementDAO findUserPlacement(String userId);
	
	void save(CourseUserPlacementDAO placementDao);
	
	public int flagSelectedCourseGroups(final String source);
	
	public int flagSelectedCourseComponents(final String source);
	
	public int flagSelectedDaisyCourseGroups(final String source);
	
	public int flagSelectedDaisyCourseComponents(final String source);
	
	public Collection<CourseGroupDAO> deleteSelectedCourseGroups(final String source);
	
	public Collection<CourseComponentDAO> deleteSelectedCourseComponents(final String source);
	
	public CourseCategoryDAO findCourseCategory(String id);
	
	void save(CourseCategoryDAO category);

}
