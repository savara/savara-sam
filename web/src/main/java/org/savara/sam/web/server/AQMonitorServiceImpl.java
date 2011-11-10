/**
 * 
 */
package org.savara.sam.web.server;

import java.util.ArrayList;
import java.util.List;

import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.server.ActiveQueryServer;
import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.dto.ResponseTime;
import org.savara.sam.web.shared.dto.Statistic;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * @author Jeff Yu
 * @date Nov 4, 2011
 */

public class AQMonitorServiceImpl extends RemoteServiceServlet implements AQMonitorService {
	
	private static final long serialVersionUID = 8965645007479773817L;
	
	private ActiveQueryManager _activeQueryManager;
	
	private ActiveQuery<ActivitySummary> _startedTxns;
	private ActiveQuery<ActivitySummary> _completedTxns;
	private ActiveQuery<ActivitySummary> _failedTxns;
	
	private ActiveQuery<ActivityAnalysis> _responseTime;
		
	public AQMonitorServiceImpl() {
		_activeQueryManager = ActiveQueryServer.getInstance();
		_startedTxns = _activeQueryManager.getActiveQuery("PurchasingStarted");
		_completedTxns = _activeQueryManager.getActiveQuery("PurchasingSuccessful");
		_failedTxns = _activeQueryManager.getActiveQuery("PurchasingUnsuccessful");
		
		_responseTime = _activeQueryManager.getActiveQuery("PurchasingResponseTime");
	}
	
	public Statistic[] getStatistics() {

		Statistic running = new Statistic();
		running.setValue(_startedTxns.size() - _completedTxns.size() - _failedTxns.size());
		running.setName("Running");
		
		Statistic successful = new Statistic();
		successful.setValue(_completedTxns.size());		
		successful.setName("Successful");
		
		Statistic failed = new Statistic();
		failed.setValue(_failedTxns.size());
		failed.setName("Unsuccessful");
		
		Statistic started = new Statistic();
		started.setName("Started");
		started.setValue(_startedTxns.size());
		
		Statistic[] result = new Statistic[]{running, successful, failed, started};
		 return result;
	}


	public ResponseTime[] getResponseTimes() {
		List<ActivityAnalysis> contents= _responseTime.getContents();
		List<ResponseTime> result = new ArrayList<ResponseTime>();
		for (ActivityAnalysis aa : contents) {
			ResponseTime rt = new ResponseTime();
			rt.setRequestTime((Long)aa.getProperty("requestTimestamp").getValue());
			rt.setResponseTime((Long)aa.getProperty("responseTime").getValue());
			result.add(rt);
		}
		ResponseTime[] rts = result.toArray(new ResponseTime[0]);
		return rts;
	}
	

}
