/**
 * 
 */
package org.savara.sam.web.server;

import java.util.ArrayList;
import java.util.List;

import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.server.ActiveQueryServer;
import org.savara.sam.conversation.ConversationDetails;
import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.dto.Conversation;
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
	
	private ActiveQuery<String> _startedTxns;
	private ActiveQuery<String> _completedTxns;
	private ActiveQuery<String> _failedTxns;
	
	private ActiveQuery<ActivityAnalysis> _responseTime;
	
	private ActiveQuery<ConversationDetails> _purchasingConversation;
		
	public AQMonitorServiceImpl() {
		_activeQueryManager = ActiveQueryServer.getInstance();
		_startedTxns = _activeQueryManager.getActiveQuery("PurchasingStarted");
		_completedTxns = _activeQueryManager.getActiveQuery("PurchasingSuccessful");
		_failedTxns = _activeQueryManager.getActiveQuery("PurchasingUnsuccessful");
		
		_responseTime = _activeQueryManager.getActiveQuery("PurchasingResponseTime");
		
		_purchasingConversation = _activeQueryManager.getActiveQuery("PurchasingConversation");
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
			rt.setOperation(aa.getProperty("operation").getValue().toString());
			result.add(rt);
		}
		ResponseTime[] rts = result.toArray(new ResponseTime[0]);
		return rts;
	}

	public Conversation[] getConversationDetails() {
		List<ConversationDetails> details  = _purchasingConversation.getContents();
		List<Conversation> result = new ArrayList<Conversation>();
		for (ConversationDetails detail : details) {
			Conversation cd = new Conversation();
			cd.setConversationId(detail.getId().getId());
			cd.setStatus(detail.isValid());
			result.add(cd);
		}
		Conversation[] cds = result.toArray(new Conversation[0]);
		return cds;
	}
	

}
