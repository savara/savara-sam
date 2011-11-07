/**
 * 
 */
package org.savara.sam.web.server;

import javax.annotation.Resource;
import javax.naming.InitialContext;

import org.infinispan.manager.CacheContainer;
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
	
	//TODO: this doesn't seem to work.
	//@Resource(mappedName="java:jboss/infinispan/sam")
	private CacheContainer _container;
	
	private ActiveQueryManager _activeQueryManager;
	
	private ActiveQuery<ActivitySummary> _startedTxns;
	private ActiveQuery<ActivitySummary> _completedTxns;
	private ActiveQuery<ActivitySummary> _failedTxns;
		
	public AQMonitorServiceImpl() {
		try {
			InitialContext context = new InitialContext();
			_container = (CacheContainer)context.lookup("java:jboss/infinispan/sam");
			_activeQueryManager = new ActiveQueryServer();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		_startedTxns = _activeQueryManager.getActiveQuery("PurchasingStarted");
		_completedTxns = _activeQueryManager.getActiveQuery("PurchasingSuccessful");
		_failedTxns = _activeQueryManager.getActiveQuery("PurchasingUnsuccessful");

	}
	
	public Statistic[] getStatistics() {

		Statistic running = new Statistic();
		if (_startedTxns != null && _completedTxns != null && _failedTxns != null) {
			running.setValue(_startedTxns.size() - _completedTxns.size() - _failedTxns.size());
		} else {
			running.setValue(0);
		}
		running.setName("Running");
		
		Statistic successful = new Statistic();
		if (_completedTxns != null) {
			successful.setValue(_completedTxns.size());
		} else {
			successful.setValue(0);
		}
		successful.setName("Successful");
		
		Statistic failed = new Statistic();
		if (_failedTxns != null) {
			failed.setValue(_failedTxns.size());
		} else {
			failed.setValue(0);
		}
		failed.setName("Unsuccessful");
		
		 Statistic[] result = new Statistic[]{running, successful, failed};
		 if (_startedTxns != null) {
			 System.out.println("===> " + _startedTxns.size());
		 }
		 return result;
	}


	public ResponseTime[] getResponseTimes() {
		
		return null;
	}
	

}
