/**
 * 
 */
package org.savara.sam.web.server;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.aq.ActiveListener;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
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
	
	@Inject
	private ActiveQueryManager _activeQueryManager;
	
	private ActiveQuery<ActivitySummary> _startedTxns;
	private ActiveQuery<ActivitySummary> _completedTxns;
	private ActiveQuery<ActivitySummary> _failedTxns;
		
	public AQMonitorServiceImpl() {
		_startedTxns = _activeQueryManager.getActiveQuery("PurchasingStarted");
		_completedTxns = _activeQueryManager.getActiveQuery("PurchasingSuccessful");
		_failedTxns = _activeQueryManager.getActiveQuery("PurchasingUnsuccessful");

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
		
		 Statistic[] result = new Statistic[]{running, successful, failed};
		 return result;
	}


	public ResponseTime[] getResponseTimes() {
		
		return null;
	}
	

}
