package uk.ac.ox.oucs.vle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


public class CourseComponentImpl implements CourseComponent {
	
	private CourseComponentDAO dao;
	private List<CourseComponentSession> componentSessions;
	
	//private CourseSignupServiceImpl impl;
	
	/// Local caches.
	private transient Date opens;
	private transient Date closes;
	private transient Date starts;
	private transient Date ends;
	private transient Date created;
	private transient Date baseDate;
	
	private static int YEARSAGO = -1825;
	
	public CourseComponentImpl(CourseComponentDAO dao, CourseSignupServiceImpl impl) {
		this.dao = dao;
		//this.impl = impl;
	}

	public String getPresentationId() {
		return dao.getPresentationId();
	}
	
	public String getSubject() {
		return dao.getSubject();
	}

	public String getTitle() {
		return dao.getTitle();
	}

	public int getPlaces() {
		return dao.getSize() - dao.getTaken();
	}

	public int getSize() {
		return dao.getSize();
	}

	public Person getPresenter() {
		if (dao.getTeacherName() != null) {
			return new PersonImpl(null, null, null, 
					dao.getTeacherName(), dao.getTeacherEmail(), 
					Collections.<String>emptyList(), 
					null, null, null, null, null, null);
		}
		return null;
	}
	
	public String getLocation() {
		return dao.getLocation();
	}

	public Date getOpens() {
		// Jackson doesn't like java.sql.Date.
		if (opens == null && dao.getOpens() != null)
			opens = new Date(dao.getOpens().getTime());
		return opens;
	}
	
	public String getOpensText() {
		return dao.getOpensText();
	}

	public Date getCloses() {
		// Jackson doesn't like java.sql.Date.
		if(closes == null && dao.getCloses() != null)
			closes = new Date(dao.getCloses().getTime());
		return dao.getCloses();
	}
	
	public String getClosesText() {
		return dao.getClosesText();
	}
	
	public Date getStarts() {
		// Jackson doesn't like java.sql.Date.
		if(starts == null && dao.getStarts() != null) {
			starts = new Date(dao.getStarts().getTime());
		}
		return starts;
	}
	
	public String getStartsText() {
		return dao.getStartsText();
	}

	public Date getEnds() {
		// Jackson doesn't like java.sql.Date.
		if(ends == null && dao.getEnds() != null) {
			ends = new Date(dao.getEnds().getTime());
		}
		return ends;
	}
	
	public String getEndsText() {
		return dao.getEndsText();
	}
	
	public Date getCreated() {
		// Jackson doesn't like java.sql.Date.
		if(created == null && dao.getCreated() != null) {
			created = new Date(dao.getCreated().getTime());
		}
		if (null == created) {
			GregorianCalendar cal = new GregorianCalendar();  
			cal.add(Calendar.DATE, YEARSAGO);   
			return cal.getTime();
		}
		return created;
	}
	
	public Date getBaseDate() {
		// Jackson doesn't like java.sql.Date.
		if(baseDate == null && dao.getBaseDate() != null) {
			baseDate = new Date(dao.getBaseDate().getTime());
		}
		if (null == baseDate) {
			GregorianCalendar cal = new GregorianCalendar();  
			cal.add(Calendar.DATE, YEARSAGO);   
			return cal.getTime();
		}
		return baseDate;
	}

	public String getComponentSet() {
		return dao.getComponentId(); 
	}

	public String getWhen() {
		return dao.getWhen();
	}

	public String getSlot() {
		return dao.getSlot();
	}

	public String getSessions() {
		return dao.getSessions();
	}
	
	public boolean getBookable() {
		return dao.isBookable();
	}

	public String getApplyTo() {
		return dao.getApplyTo();
	}

	public String getMemberApplyTo() {
		return dao.getMemberApplyTo();
	}
	
	public String getAttendanceMode() {
		return dao.getAttendanceMode();
	}
	
	public String getAttendanceModeText() {
		return dao.getAttendanceModeText();
	}
	
	public String getAttendancePattern() {
		return dao.getAttendancePattern();
	}
	
	public String getAttendancePatternText() {
		return dao.getAttendancePatternText();
	}
	
	public String getSource() {
		return dao.getSource();
	}
	
	public List<CourseComponentSession> getComponentSessions() {
		if (componentSessions == null) {
			componentSessions = new ArrayList<CourseComponentSession>();
			for(CourseComponentSessionDAO session:  dao.getComponentSessions()) {
				componentSessions.add(new CourseComponentSessionImpl(session));
			}
		}
		return componentSessions;
	}
}
