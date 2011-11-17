/**
 * 
 */
package org.savara.sam.web.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.dto.AQChartModel;
import org.savara.sam.web.shared.dto.Conversation;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * 
 * This is a stub GWT servlet service
 * 
 * @author Jeff Yu
 * @date Nov 9, 2011
 */
public class AQMonitorServiceMock extends RemoteServiceServlet implements AQMonitorService {


	public Conversation[] getConversationDetails() {
		
		Date d = new Date();
		Conversation detail = new Conversation();
		
		detail.setConversationId("id4836668026842032602");
		detail.setStatus(true);
		detail.setUpdatedDate(d.getTime());
		
		Conversation detail2 = new Conversation();
		detail2.setConversationId("id4836668026842032603");
		detail2.setStatus(false);
		detail2.setUpdatedDate(d.getTime() + 1500);
		
		Conversation detail3 = new Conversation();
		detail3.setConversationId("id4836668026842072602");
		detail3.setStatus(true);
		detail3.setUpdatedDate(d.getTime() + 2000);
		
		return new Conversation[]{detail, detail2, detail3};
	}

	public List<String> getSystemAQNames() {
		List<String> aqNames = new ArrayList<String>();
		aqNames.add("PurchasingStarted");
		aqNames.add("PurchasingSuccessful");
		aqNames.add("PurchasingUnsuccessful");
		aqNames.add("PurchasingResponseTime");
		aqNames.add("PurchasingConversation");
		return aqNames;
	}
	
	@SuppressWarnings("unchecked")
	public Map getChartData(AQChartModel model) {
		List<String> aqNames = model.getActiveQueryNames();
		Map result = new HashMap();
		
		long today = new Date().getTime();
		for (String aq : model.getActiveQueryNames()) {
			if ("size".equals(model.getVerticalProperty()) && "name".equals(model.getHorizontalProperty())) {
				if ("PurchasingStarted".equals(aq)) result.put("PurchasingStarted", new Integer(20));
				else if ("PurchasingSuccessful".equals(aq)) result.put("PurchasingSuccessful", new Integer(18));
				else if ("PurchasingUnsuccessful".equals(aq)) result.put("PurchasingUnsuccessful", new Integer(2));
			} else if ("requestTimestamp".equals(model.getHorizontalProperty()) && "responseTime".equals(model.getVerticalProperty())) {
				result.put(new Long(today), new Long(980));
				result.put(new Long(today + 1000), new Long(980));
				result.put(new Long(today + 2000), new Long(990));
				result.put(new Long(today + 3500), new Long(790));
				result.put(new Long(today + 4000), new Long(890));
				result.put(new Long(today + 4600), new Long(290));
				result.put(new Long(today + 6400), new Long(1090));
				result.put(new Long(today + 8000), new Long(9001));
			} else {
				throw new UnsupportedOperationException("Unsupported operations for now");
			}
		}	
		return result;
	}

}
