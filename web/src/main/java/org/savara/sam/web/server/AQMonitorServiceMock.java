/**
 * 
 */
package org.savara.sam.web.server;

import java.util.Date;

import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.dto.ResponseTime;
import org.savara.sam.web.shared.dto.Statistic;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * 
 * This is a stub GWT servlet service
 * 
 * @author Jeff Yu
 * @date Nov 9, 2011
 */
public class AQMonitorServiceMock extends RemoteServiceServlet implements AQMonitorService {

	/* (non-Javadoc)
	 * @see org.savara.sam.web.shared.AQMonitorService#getStatistics()
	 */
	public Statistic[] getStatistics() {
		Statistic successful = new Statistic();
		successful.setValue(18);		
		successful.setName("Successful");
		
		Statistic failed = new Statistic();
		failed.setValue(2);
		failed.setName("Unsuccessful");
		
		Statistic started = new Statistic();
		started.setName("Started");
		started.setValue(20);
		
		Statistic[] result = new Statistic[]{successful, failed, started};
		return result;
	}

	/* (non-Javadoc)
	 * @see org.savara.sam.web.shared.AQMonitorService#getResponseTimes()
	 */
	public ResponseTime[] getResponseTimes() {
		ResponseTime first = new ResponseTime();
		first.setRequestTime(new Date().getTime());
		first.setResponseTime(Long.valueOf(980));
		
		ResponseTime second = new ResponseTime();
		second.setRequestTime(new Date().getTime() + 1000);
		second.setResponseTime(Long.valueOf(990));
		
		ResponseTime[] result = new ResponseTime[]{first, second};
		return result;
	}

}
