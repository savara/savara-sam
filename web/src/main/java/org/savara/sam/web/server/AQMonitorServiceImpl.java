/**
 * 
 */
package org.savara.sam.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savara.monitor.ConversationId;
import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.activity.Situation;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.ActiveQuerySpec;
import org.savara.sam.aqs.ActiveQueryServer;
import org.savara.sam.activity.ConversationDetails;
import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.dto.AQChartModel;
import org.savara.sam.web.shared.dto.Conversation;
import org.savara.sam.web.shared.dto.SituationDTO;

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
	
	private ActiveQuery<ConversationId> _purchasingConversation;
	private ActiveQuerySpec _purchasingConversationSpec;
	
	private ActiveQuery<?> _activeQuery;
		
	public AQMonitorServiceImpl() {
		_activeQueryManager = ActiveQueryServer.getInstance();
		_startedTxns = _activeQueryManager.getActiveQuery("PurchasingStarted");
		_completedTxns = _activeQueryManager.getActiveQuery("PurchasingSuccessful");
		_failedTxns = _activeQueryManager.getActiveQuery("PurchasingUnsuccessful");
		
		_responseTime = _activeQueryManager.getActiveQuery("PurchasingResponseTime");
		
		_purchasingConversation = _activeQueryManager.getActiveQuery("PurchasingConversation");
	}
	
	public List<String> getSystemAQNames() {
		List<String> aqNames = new ArrayList<String>();
		Collection<ActiveQuerySpec> aqSpecs = _activeQueryManager.getActiveQueries();
		for (ActiveQuerySpec spec : aqSpecs) {
			aqNames.add(spec.getName());
		}
		return aqNames;
	}

	public Conversation[] getConversationDetails() {
		if (_purchasingConversationSpec == null) {
			_purchasingConversationSpec = _activeQueryManager.getActiveQuerySpec("PurchasingConversation");
		}
		
		List<ConversationId> cids  = _purchasingConversation.getContents();
		List<Conversation> result = new ArrayList<Conversation>();
		for (ConversationId cid : cids) {
			ConversationDetails detail=(ConversationDetails)_purchasingConversationSpec.resolve(cid);
			
			if (detail != null) {
				Conversation cd = new Conversation();
				cd.setConversationId(detail.getId().getId());
				cd.setStatus(detail.isValid());
				cd.setUpdatedDate(detail.getEndTimestamp());
				result.add(cd);
			} else {
				System.err.println("FAILED TO GET CONVERSATION DETAILS FOR CID="+cid);
			}

		}
		Conversation[] cds = result.toArray(new Conversation[result.size()]);
		return cds;
	}
	
	@SuppressWarnings("unchecked")
	public Map getChartData(AQChartModel model) {
		List<String> aqNames = model.getActiveQueryNames();
		Map result = new HashMap();
		
		for (String aqName :aqNames) {
			_activeQuery = _activeQueryManager.getActiveQuery(aqName);
			if ("size".equals(model.getVerticalProperty()) && "name".equals(model.getHorizontalProperty())) {
				result.put(aqName, new Integer(_activeQuery.size()));
			} else if ("requestTimestamp".equals(model.getHorizontalProperty()) && "responseTime".equals(model.getVerticalProperty())) {
				List<?> content = _activeQuery.getContents();
				for (Object o : content) {
					ActivityAnalysis aa  = (ActivityAnalysis) o;
					result.put((Long)aa.getProperty("requestTimestamp").getValue(), (Long)aa.getProperty("responseTime").getValue());
				}
			} else {
				throw new UnsupportedOperationException("Unsupported operations for now");
			}
		}
			
		return result;
	}

	public List<SituationDTO> getSituations() {
		ActiveQuery<Situation> situations = _activeQueryManager.getActiveQuery("Situations");
		List<SituationDTO> result = new ArrayList<SituationDTO>();
		
		if (situations != null) {
			for (Situation situation : situations.getContents()) {
				SituationDTO dto = new SituationDTO();
				dto.setId(situation.getId());
				Date today = new Date();
				today.setTime(situation.getCreatedTimestamp());
				dto.setCreatedDate(today);
				dto.setDescription(situation.getDescription());
				dto.setExternalRef(situation.getExternalReference());
				dto.setOwner(situation.getOwner());
				dto.setPrincipal(situation.getPrincipal());
				dto.setPriority(situation.getPriority().toString());
				dto.setSeverity(situation.getSeverity().toString());
				dto.setStatus(situation.getStatus().toString());
				
				result.add(dto);
			}
		}
		
		return result;
	}


	

}
