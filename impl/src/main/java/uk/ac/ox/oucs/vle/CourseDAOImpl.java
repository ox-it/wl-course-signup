package uk.ac.ox.oucs.vle;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinFragment;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import uk.ac.ox.oucs.vle.CourseSignupService.Range;
import uk.ac.ox.oucs.vle.CourseSignupService.Status;

public class CourseDAOImpl extends HibernateDaoSupport implements CourseDAO {

	
	public CourseGroupDAO findCourseGroupById(final String courseId) {
		return (CourseGroupDAO) getHibernateTemplate().execute(new HibernateCallback() {
			// Need the DISTINCT ROOT ENTITY filter.
			public Object doInHibernate(Session session) 
					throws HibernateException,	SQLException {

				StringBuffer querySQL = new StringBuffer();
				querySQL.append("SELECT * FROM course_group cg ");
				querySQL.append("WHERE cg.courseId = :id ");
				Query query = session.createSQLQuery(querySQL.toString()).addEntity(CourseGroupDAO.class);
				query.setString("id", courseId);
				List<CourseGroupDAO> courseGroups =  query.list();
				int results = courseGroups.size();
				if (results > 0) {
					if (results > 1) {
						throw new IllegalStateException("To many results ("+ results + ") found for "+ courseId );
					}
					return courseGroups.get(0);
				}
				return null;
			}
		});
	}
	
	public CourseGroupDAO findCourseGroupById(final String courseId, final Range range, final Date now) {
		return (CourseGroupDAO) getHibernateTemplate().execute(new HibernateCallback() {
			// Need the DISTINCT ROOT ENTITY filter.
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				Criteria criteria = session.createCriteria(CourseGroupDAO.class);
				criteria.add(Expression.eq("courseId", courseId));
				criteria.add(Restrictions.eq("hideGroup", false));
				switch (range) { 
					case NOTSTARTED:
						criteria = criteria.createCriteria("components", JoinFragment.LEFT_OUTER_JOIN).add(
								Expression.or(Expression.gt("baseDate", now), Expression.and(Expression.isNull("baseDate"), Expression.isNotNull("startsText"))));
						break;
					case UPCOMING:
						criteria = criteria.createCriteria("components", JoinFragment.LEFT_OUTER_JOIN).add(
								Expression.or(Expression.gt("baseDate", now), Expression.and(Expression.isNull("baseDate"), Expression.isNotNull("startsText"))));
						break;
					case PREVIOUS:
						criteria = criteria.createCriteria("components",  JoinFragment.LEFT_OUTER_JOIN).add(
								Expression.or(Expression.le("baseDate", now), Expression.and(Expression.isNull("baseDate"), Expression.isNull("startsText"))));
						break;
				}
				criteria.setResultTransformer(Criteria.ROOT_ENTITY);
				return criteria.uniqueResult();
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public CourseGroupDAO findUpcomingComponents(String courseId, Date available) {
		List<CourseGroupDAO> courseGroups = getHibernateTemplate().findByNamedParam(
				"select distinct cg from CourseGroupDAO cg left join fetch cg.components as component where cg.courseId = :courseId and component.closes > :closes",
				new String[]{"courseId", "closes"}, new Object[]{courseId, available});
		int results = courseGroups.size();
		if (results > 0) {
			if (results > 1) {
				throw new IllegalStateException("To many results ("+ results + ") found for "+ courseId );
			}
			return courseGroups.get(0);
		}
		return null;
	}

	public List<CourseComponentDAO> findOpenComponents(String id, Date at) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseComponentDAO> findAllComponents() {
		return getHibernateTemplate().loadAll(CourseComponentDAO.class);
	}

	public CourseGroupDAO findAvailableCourseGroupById(String courseId) {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<CourseGroupDAO> findCourseGroupByDept(final String deptId, final Range range, final Date now, final boolean external) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			// Need the DISTINCT ROOT ENTITY filter.
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				
				StringBuffer querySQL = new StringBuffer();
				querySQL.append("SELECT DISTINCT cg.muid, cg.courseId, cg.title, cg.dept, cg.departmentName, ");
				querySQL.append("cg.subunit, cg.subunitName, cg.description, ");
				querySQL.append("cg.supervisorApproval, cg.administratorApproval, cg.hideGroup, ");
				querySQL.append("cg.deleted, cg.contactEmail, cg.regulations, cg.visibility, cg.source FROM course_group cg ");
				querySQL.append("LEFT JOIN course_group_otherDepartment cgd on cgd.courseGroupMuid = cg.muid ");
				querySQL.append("LEFT JOIN course_group_component cgc on cgc.courseGroupMuid = cg.muid ");
				querySQL.append("LEFT JOIN course_component cc on cgc.courseComponentMuid = cc.muid ");
				querySQL.append("WHERE ");
				
				querySQL.append("visibility != 'PR' AND ");
				
				if (external) {
					querySQL.append("visibility != 'RS' AND ");
				}

				querySQL.append("hideGroup = false AND ");
				
				switch (range) { 
					case UPCOMING:
						querySQL.append("((cc.baseDate is null AND cc.startsText is not null) OR cc.baseDate > now()) AND ");
						break;
					case PREVIOUS:
						querySQL.append("((cc.baseDate is null AND cc.startsText is null) OR cc.baseDate <= now()) AND ");
						break;
				}
				
				querySQL.append("(otherDepartment = :deptId ");
				querySQL.append("OR (dept = :deptId and (subunit is NULL or subunit = ''))) ");
				querySQL.append("ORDER BY cg.title ");
				
				Query query = session.createSQLQuery(querySQL.toString()).addEntity(CourseGroupDAO.class);
				query.setString("deptId", deptId);
				return query.list();
			}	
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseGroupDAO> findCourseGroupBySubUnit(final String subunitId, final Range range, final Date now, final boolean external) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			// Need the DISTINCT ROOT ENTITY filter.
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				
				Criteria criteria = session.createCriteria(CourseGroupDAO.class);
				criteria.add(Restrictions.eq("subunit", subunitId));
				criteria.add(Restrictions.ne("visibility", "PR"));
				if (external) {
					criteria.add(Restrictions.ne("visibility", "RS"));
				}
				criteria.add(Restrictions.eq("hideGroup", false));
				switch (range) { 
					case UPCOMING:
						criteria = criteria.createCriteria("components", JoinFragment.LEFT_OUTER_JOIN).add(
								Expression.or(Expression.gt("baseDate", now), Expression.and(Expression.isNull("baseDate"), Expression.isNotNull("startsText"))));
						break;
					case PREVIOUS:
						criteria = criteria.createCriteria("components",  JoinFragment.LEFT_OUTER_JOIN).add(
								Expression.or(Expression.le("baseDate", now), Expression.and(Expression.isNull("baseDate"), Expression.isNull("startsText"))));
						break;
				}
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				criteria.addOrder(Order.asc("title"));
				return criteria.list();
			}
			
		});
	}
	
	/**
	 * Find all courseGroups that share a component with Id componentId
	 */
	@SuppressWarnings("unchecked")
	public List<CourseGroupDAO> findCourseGroupByComponent(final String componentId) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select * from course_component " +
						"left join course_group_component on course_group_component.courseComponentMuid = course_component.muid " +
						"left join course_group on course_group_component.courseGroupMuid = course_group.muid " +
						"where course_component.presentationId = :componentId").addEntity(CourseGroupDAO.class);
				query.setString("componentId", componentId);
				return query.list();
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<Object[]> findSubUnitByDept(final String deptId) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			// Need the DISTINCT ROOT ENTITY filter.
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				Query query = session.createQuery("select distinct subunit, subunitName " +
						"from CourseGroupDAO cg " +
						"where cg.dept = :deptId and cg.subunit <> '' order by 2");
				query.setString("deptId", deptId);
				return query.list();
			}
			
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseDepartmentDAO> findAllDepartments() {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			// Need the DISTINCT ROOT ENTITY filter.
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				Query query = session.createSQLQuery("select * from course_department").addEntity(CourseDepartmentDAO.class);
				return query.list();
			}
			
		});
	}

	public CourseComponentDAO findCourseComponent(final String id) {
		return (CourseComponentDAO) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException,
					SQLException {
				Criteria criteria = session.createCriteria(CourseComponentDAO.class);
				criteria.add(Expression.eq("presentationId", id));
				criteria.setResultTransformer(Criteria.ROOT_ENTITY);
				return criteria.uniqueResult();
			}
		});
	}

	public CourseSignupDAO newSignup(String userId, String supervisorId) {
		CourseSignupDAO signupDao = new CourseSignupDAO();
		signupDao.setUserId(userId);
		signupDao.setSupervisorId(supervisorId);
		return signupDao;
	}

	public String save(CourseSignupDAO signupDao) {
		return getHibernateTemplate().save(signupDao).toString();
	}

	public String save(CourseComponentDAO componentDao) {
		return getHibernateTemplate().save(componentDao).toString();
	}

	public CourseSignupDAO findSignupById(String signupId) {
		return (CourseSignupDAO) getHibernateTemplate().get(CourseSignupDAO.class, signupId);
	}
	
	public CourseSignupDAO findSignupByEncryptId(String signupId) {
		return (CourseSignupDAO) getHibernateTemplate().get(CourseSignupDAO.class, signupId);
	}

	@SuppressWarnings("unchecked")
	public List<CourseSignupDAO> findSignupForUser(final String userId, final Set<Status> statuses) {
		return (List<CourseSignupDAO>)getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(CourseSignupDAO.class);
				criteria.add(Expression.eq("userId", userId));
				if (!statuses.isEmpty()) {
					criteria.add(Expression.in("status", statuses.toArray()));
				}
				criteria.setFetchMode("components", FetchMode.JOIN);
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				return criteria.list();
			}
		});
	}

	public CourseGroupDAO newCourseGroup(String id, String title, String dept, String subunit) {
		CourseGroupDAO groupDao = new CourseGroupDAO();
		groupDao.setCourseId(id);
		groupDao.setTitle(title);
		groupDao.setDept(dept);
		groupDao.setSubunit(subunit);
		return groupDao;
	}

	public void save(CourseGroupDAO groupDao) {
		getHibernateTemplate().save(groupDao);
	}

	@SuppressWarnings("unchecked")
	public List<CourseGroupDAO> findAdminCourseGroups(final String userId) {
		// Finds all the coursegroups this user can admin.
		List<CourseGroupDAO> adminGroups = findAdministratorCourseGroups(userId);
		List<CourseGroupDAO> superGroups = findSuperUserCourseGroups(userId);
		
		Set<CourseGroupDAO> allGroups = new HashSet<CourseGroupDAO>(superGroups);
		allGroups.addAll(adminGroups);
		return new ArrayList<CourseGroupDAO>(allGroups);
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseGroupDAO> findAdministratorCourseGroups(final String userId) {
		// Finds all the coursegroups this user can admin. 
		return getHibernateTemplate().executeFind(new HibernateCallback(){
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				Query query = session.createSQLQuery("select * from course_group, " +
					"(select courseGroupMuid from course_group_administrator " +
					"where administrator = :userId) admins " +
					"where course_group.muid = admins.courseGroupMuid " +
					"and course_group.hideGroup = false").addEntity(CourseGroupDAO.class);
				query.setString("userId", userId);
				return query.list();
			}
			
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseGroupDAO> findSuperUserCourseGroups(final String userId) {
		// Finds all the coursegroups this user can superuser.
		return getHibernateTemplate().executeFind(new HibernateCallback(){
			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				Query query = session.createSQLQuery("select * from course_group, " +
					"(select courseGroupMuid from course_group_superuser " +
					"where superuser = :userId) admins " +
					"where course_group.muid = admins.courseGroupMuid").addEntity(CourseGroupDAO.class);
				query.setString("userId", userId);
				return query.list();
			}
			
		});
	}

	@SuppressWarnings("unchecked")
	public List<CourseSignupDAO> findSignupByCourse(final String userId, final String courseId, final Set<Status> statuses) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {

			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				Query query;
				if (null != statuses && !statuses.isEmpty()) {
					/*
					query = session.createSQLQuery("select * from course_signup, " +
							"(select course_group from course_group_administrator " +
							"where administrator = :userId " +
							"union select course_group from course_group_superuser " +
							"where superuser = :userId) admins " +
							"where course_signup.groupId = admins.course_group " +
							"and course_signup.groupId = :courseId " +
							"and course_signup.status in (:statuses)").addEntity(CourseSignupDAO.class);
					*/
					query = session.createSQLQuery("select * from course_signup " +
							"left join course_group on course_signup.courseGroupMuid = course_group.muid " +
							"where course_group.courseId = :courseId " +
							"and course_signup.status in (:statuses)").addEntity(CourseSignupDAO.class);
					
					query.setParameterList("statuses", statuses);
				} else {
					/*
					query = session.createSQLQuery("select * from course_signup, " +
							"(select course_group from course_group_administrator " +
							"where administrator = :userId " +
							"union select course_group from course_group_superuser " +
							"where superuser = :userId) admins " +
							"where course_signup.groupId = admins.course_group " +
							"and course_signup.groupId = :courseId").addEntity(CourseSignupDAO.class);
					*/
					query = session.createSQLQuery("select * from course_signup " +
							"left join course_group on course_signup.courseGroupMuid = course_group.muid " +
							"where course_group.courseId = :courseId").addEntity(CourseSignupDAO.class);
				}
				//query.setString("userId", userId);
				query.setString("courseId", courseId);
				return query.list();
			}
			
		});
	}
	
	@SuppressWarnings("unchecked")
	public Integer countSignupByCourse(final String courseId, final Set<Status> statuses) {
		return (Integer)getHibernateTemplate().execute(new HibernateCallback() {

			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				Query query = session.createSQLQuery("select count(*) from course_signup " +
							"left join course_component_signup on course_component_signup.signup = course_signup.id " +
							"left join course_component on course_component.muid = course_component_signup.courseComponentMuid " +
							"left join course_group on course_group.muid = course_signup.courseGroupMuid " +
							"where course_group.courseId = :courseId " +
							"and course_component.starts > NOW() " +
							"and course_signup.status in (:statuses)");
					
				query.setParameterList("statuses", statuses);
				query.setString("courseId", courseId);
				List<Object> results = query.list();
				int count = results.size();
				if (count > 0) {
					if (count > 1) {
						throw new IllegalStateException("To many results ("+ results + ") found for "+ courseId );
					}
					return ((BigInteger)results.get(0)).intValue();
				}
				return null;
			}
			
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseSignupDAO> findSignupByComponent(final String componentId, final Set<Status> statuses) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {

			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				Query query;
				if (null != statuses && !statuses.isEmpty()) {
					query = session.createQuery(
							"select cs from CourseSignupDAO cs " +
							"inner join fetch cs.components cc " +
							"where cc.presentationId = :componentId and cs.status in (:statuses)");
					query.setParameterList("statuses", statuses);
				} else {
					query = session.createQuery(
							"select cs from CourseSignupDAO cs " +
							"inner join fetch cs.components cc " +
							"where cc.presentationId = :componentId");
				}
				
				query.setString("componentId", componentId);
				return query.list();
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseSignupDAO> findSignupPending(final String userId) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select distinct cs.id, cs.userId, cs.status, cs.created, cs.amended, cs.message, cs.supervisorId, cs.courseGroupMuid, cs.department " +
						"from course_signup cs " +
						"left join course_group_administrator ca on cs.courseGroupMuid = ca.courseGroupMuid " +
						"inner join course_component_signup cp on cs.id = cp.signup " +
						"inner join course_component cc on cp.courseComponentMuid = cc.muid " +
						"where (ca.administrator = :userId and cs.status = :adminStatus) or (cs.supervisorId = :userId and cs.status = :supervisorStatus)").addEntity(CourseSignupDAO.class);
				query.setString("userId", userId);
				query.setParameter("adminStatus", Status.PENDING.name());
				query.setParameter("supervisorStatus", Status.ACCEPTED.name());
				return query.list();
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseSignupDAO> findSignupStillPendingOrAccepted(final Integer period) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select distinct cs.id, cs.userId, cs.status, cs.created, cs.amended, cs.message, cs.supervisorId, cs.courseGroupMuid, cs.department " +
						"from course_signup cs " +
						"left join course_group_administrator ca on cs.courseGroupMuid = ca.courseGroupMuid " +
						"inner join course_component_signup cp on cs.id = cp.signup " +
						"inner join course_component cc on cp.courseComponentMuid = cc.muid " +
						"where (date_sub(curdate(), interval :period day) >= cs.amended " +
						"or date_sub(curdate(), interval :period day) <= cc.starts) " +
						"and (curdate() < cc.starts) " +
						"and ((cs.status = :adminStatus) or (cs.status = :supervisorStatus))").addEntity(CourseSignupDAO.class);
				query.setInteger("period", period);
				query.setParameter("adminStatus", Status.PENDING.name());
				query.setParameter("supervisorStatus", Status.ACCEPTED.name());
				return query.list();
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public List<CourseSignupDAO> findSignupApproval(final String userId) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select distinct cs.id, cs.userId, cs.status, cs.created, cs.amended, cs.message, cs.supervisorId, cs.courseGroupMuid, cs.department " +
						"from course_signup cs " +
						"left join course_group_administrator ca on cs.courseGroupMuid = ca.courseGroupMuid " +
						"inner join course_component_signup cp on cs.id = cp.signup " +
						"inner join course_component cc on cp.courseComponentMuid = cc.muid " +
						"inner join course_department_approver da on da.department = cs.department " +
						"where da.approver = :userId and cs.status = :approverStatus").addEntity(CourseSignupDAO.class);
				query.setString("userId", userId);
				query.setParameter("approverStatus", Status.APPROVED.name());
				return query.list();
			}
		});
	}

	public CourseComponentDAO newCourseComponent(String id) {
		CourseComponentDAO componentDao = new CourseComponentDAO();
		componentDao.setPresentationId(id);
		Calendar now = GregorianCalendar.getInstance();
		componentDao.setCreated(now.getTime());
		return componentDao;
	}

	@SuppressWarnings("unchecked")
	public List<CourseGroupDAO> findCourseGroupByWords(final String[] words, 
			final Range range, final Date date, final boolean external) {
		
		return getHibernateTemplate().executeFind(new HibernateCallback() {

			public Object doInHibernate(Session session)
					throws HibernateException, SQLException {
				Criteria criteria = session.createCriteria(CourseGroupDAO.class);
				for(String word: words) {
					criteria.add(Expression.ilike("title", word, MatchMode.ANYWHERE));
				}
				criteria.add(Expression.ne("visibility", "PR"));
				if (external) {
					criteria.add(Expression.ne("visibility", "RS"));
				}
				criteria.add(Expression.eq("hideGroup", false));
				
				switch(range) {
					case UPCOMING:
						criteria = criteria.createCriteria("components", JoinFragment.LEFT_OUTER_JOIN).add(Expression.gt("closes", date));
						break;
					case PREVIOUS:
						criteria = criteria.createCriteria("components",  JoinFragment.LEFT_OUTER_JOIN).add(Expression.le("closes", date));
						break;
				}
				criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
				return criteria.list();
			}
			
		});
		
	}

	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public List<CourseDepartmentDAO> findApproverDepartments(final String userId) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select * from course_department_approver " +
						"left join course_department on course_department.code = course_department_approver.department " +
						"where approver = :userId").addEntity(CourseDepartmentDAO.class);
				query.setString("userId", userId);
				return query.list();
			}
		});
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findDepartmentApprovers(final String department) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select approver from course_department_approver " +
						"where department = :deptId");
				query.setString("deptId", department);
				return query.list();
			}
		});
	}
	
	/**
	 * 
	 */
	public CourseDepartmentDAO findDepartmentByCode(String code) {
		return (CourseDepartmentDAO) getHibernateTemplate().get(CourseDepartmentDAO.class, code);
	}
	
	/**
	 * 
	 */
	public void save(CourseDepartmentDAO departmentDao) {
		getHibernateTemplate().save(departmentDao).toString();
	}
	
	/**
	 * 
	 */
	public CourseSubunitDAO findSubunitByCode(String code) {
		return (CourseSubunitDAO) getHibernateTemplate().get(CourseSubunitDAO.class, code);
	}
	
	/**
	 * 
	 */
	public void save(CourseSubunitDAO subunitDao) {
		getHibernateTemplate().save(subunitDao).toString();
	}
	
	/**
	 * 
	 */
	public CourseOucsDepartmentDAO findOucsDeptByCode(String code) {
		return (CourseOucsDepartmentDAO) getHibernateTemplate().get(CourseOucsDepartmentDAO.class, code);
	}
	
	/**
	 * select departmentCode from course_subunit left join course_oucs_department on t2Char = subunitCode where oucsCode = 'histfac'
	 */
	@SuppressWarnings("unchecked")
	public CourseDepartmentDAO findDepartmentByPrimaryOrgUnit(final String primaryOrgUnit) {
		
		List<Object> results = getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select * from course_department " +
						"left join course_subunit on course_subunit.departmentCode = course_department.code " +
						"left join course_oucs_department on t2Char = subunitCode " +
						"where oucsCode = :oucsDept").addEntity(CourseDepartmentDAO.class);
				query.setString("oucsDept", primaryOrgUnit);
				return query.list();
			}
		});
		if (!results.isEmpty()) {
			return (CourseDepartmentDAO)results.get(0);
		}
		return null;
	}
	
	/**
	 * 
	 */
	public void save(CourseOucsDepartmentDAO oucsDao) {
		getHibernateTemplate().save(oucsDao).toString();
	}
	
	public void remove(CourseSignupDAO existingSignup) {
		getHibernateTemplate().delete(existingSignup);
	}

	/**
	 * Used by tests to simulate another request being made.
	 */
	public void flush() {
		getHibernateTemplate().flush();
	}

	@SuppressWarnings("unchecked")
	public CourseUserPlacementDAO findUserPlacement(final String userId) {
		List<Object> results = getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createSQLQuery(
						"select * from course_user_placement " +
						"where userId = :userId").addEntity(CourseUserPlacementDAO.class);
				query.setString("userId", userId);
				return query.list();
			}
		});
		if (!results.isEmpty()) {
			return (CourseUserPlacementDAO)results.get(0);
		}
		return null;
	}

	public void save(CourseUserPlacementDAO placementDao) {
		getHibernateTemplate().save(placementDao).toString();
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public List<CourseComponentDAO> findCourseGroupsByCalendar(final boolean external, final String providerId) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				StringBuffer querySQL = new StringBuffer();
				querySQL.append("select distinct * from course_component cc ");
				querySQL.append("left join course_group_component cgc on cgc.courseComponentMuid = cc.muid ");
				querySQL.append("left join course_group cg on cgc.courseGroupMuid = cg.muid ");
				querySQL.append("where cc.starts > NOW() and cg.hideGroup = false ");
				querySQL.append("and cg.visibility != 'PR' ");
				if (external) {
					querySQL.append("and cg.visibility != 'RS' ");
				}
				Query query = session.createSQLQuery(querySQL.toString()).addEntity(CourseComponentDAO.class);
				return query.list();
			}
		});
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public List<CourseComponentDAO> findCourseGroupsByNoDates(final boolean external, String providerId) {
		return getHibernateTemplate().executeFind(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				StringBuffer querySQL = new StringBuffer();
				querySQL.append("select distinct * from course_component cc ");
				querySQL.append("left join course_group_component cgc on cgc.courseComponentMuid = cc.muid ");
				querySQL.append("left join course_group cg on cgc.courseGroupMuid = cg.muid ");
				querySQL.append("where cc.starts is NULL and ");
				querySQL.append("(cc.closes > NOW() or (cc.closes is null and cc.startsText is not null)) and ");
				querySQL.append("cg.hideGroup = false ");
				querySQL.append("and cg.visibility != 'PR' ");
				if (external) {
					querySQL.append("and cg.visibility != 'RS' ");
				}
				Query query = session.createSQLQuery(querySQL.toString()).addEntity(CourseComponentDAO.class);
				return query.list();
			}
		});
	}
	
	/**
	 * 
	 */
	public int flagSelectedCourseGroups(final String source) {
		return (Integer) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				StringBuffer querySQL = new StringBuffer();
				querySQL.append("update course_group ");
				querySQL.append("set deleted = true ");
				querySQL.append("where source = :source");
				Query query = session.createSQLQuery(querySQL.toString()).setString("source", source);
				return query.executeUpdate();
			}
		});
	}
	
	/**
	 * 
	 */
	public int flagSelectedCourseComponents(final String source) {
		return (Integer) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				StringBuffer querySQL = new StringBuffer();
				querySQL.append("update course_component ");
				querySQL.append("set deleted = true ");
				querySQL.append("where source = :source");
				Query query = session.createSQLQuery(querySQL.toString()).setString("source", source);
				return query.executeUpdate();
			}
		});
	}
	
	/**
	 * 
	 */
	public int flagSelectedDaisyCourseGroups(final String source) {
		return (Integer) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				StringBuffer querySQL = new StringBuffer();
				querySQL.append("update course_group cg ");
				querySQL.append("left join course_group_component cgc on cg.muid = cgc.courseGroupMuid ");
				querySQL.append("left join course_component cc on cgc.courseComponentMuid = cc.muid ");
				querySQL.append("left join course_signup cs on cg.muid = cs.courseGroupMuid ");
				querySQL.append("set cg.deleted = true ");
				querySQL.append("where cg.source = :source and ");
				querySQL.append("cc.baseDate > now() and ");
				querySQL.append("(select count(cs.id) = 0)");
				Query query = session.createSQLQuery(querySQL.toString()).setString("source", source);
				return query.executeUpdate();
			}
		});
	}
	
	/**
	 * 
	 */
	public int flagSelectedDaisyCourseComponents(final String source) {
		return (Integer) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				StringBuffer querySQL = new StringBuffer();
				querySQL.append("update course_component cc ");
				querySQL.append("left join course_component_signup ccs on cc.muid = ccs.courseComponentMuid ");
				querySQL.append("left join course_signup cs on ccs.signup = cs.id ");
				querySQL.append("set deleted = true ");
				querySQL.append("where source = :source and ");
				querySQL.append("baseDate > now() and ");
				querySQL.append("(select count(cs.id) = 0)");
				Query query = session.createSQLQuery(querySQL.toString()).setString("source", source);
				return query.executeUpdate();
			}
		});
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public Collection<CourseGroupDAO> deleteSelectedCourseGroups(final String source) {
		return (Collection<CourseGroupDAO>)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) 
					throws HibernateException,	SQLException {
				
				Criteria criteria = session.createCriteria(CourseGroupDAO.class);
				criteria.add(Restrictions.eq("deleted", true));
				criteria.add(Restrictions.eq("source", source));
				List<CourseGroupDAO> groupDaos =  criteria.list();
				for (CourseGroupDAO groupDao : groupDaos) {
					session.delete(groupDao);
				}
				return groupDaos;
			}
		});
		
	}
	
	@SuppressWarnings("unchecked")
	public Collection<CourseComponentDAO> deleteSelectedCourseComponents(final String source) {
		return (Collection<CourseComponentDAO>)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) 
					throws HibernateException,	SQLException {
				
				Criteria criteria = session.createCriteria(CourseComponentDAO.class);
				criteria.add(Restrictions.eq("deleted", true));
				criteria.add(Restrictions.eq("source", source));
				List<CourseComponentDAO> componentDaos =  criteria.list();
				for (CourseComponentDAO componentDao : componentDaos) {
					session.delete(componentDao);
				}
				return componentDaos;
			}
		});
	}
}


