package uk.ac.ox.oucs.vle;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Simple bean that allows the populator to be called from Quartz.
 * @author buckett
 *
 */
public class PopulatorJob implements Job {

	private PopulatorWrapper populatorWrapper;
	
	public void setPopulatorWrapper(PopulatorWrapper populatorWrapper){
		this.populatorWrapper = populatorWrapper;
	}
	
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		
		JobDataMap jobDataMap = context.getMergedJobDataMap();
		PopulatorContext pContext = new PopulatorContext();
		pContext.setURI((String)jobDataMap.get("xcri.oxcap.populator.uri"));
		pContext.setUser((String)jobDataMap.get("xcri.oxcap.populator.username"));
		pContext.setPassword((String)jobDataMap.get("xcri.oxcap.populator.password"));
		pContext.setName((String)jobDataMap.get("xcri.oxcap.populator.name"));
		
		populatorWrapper.update(pContext);
	}

}
