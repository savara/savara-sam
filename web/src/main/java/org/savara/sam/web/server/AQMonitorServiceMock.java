/**
 * 
 */
package org.savara.sam.web.server;

import java.util.Date;

import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.dto.Conversation;
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
		first.setOperation("buy");
		first.setResponseTime(Long.valueOf(980));
		
		ResponseTime second = new ResponseTime();
		second.setRequestTime(new Date().getTime() + 1000);
		second.setOperation("deliver");
		second.setResponseTime(Long.valueOf(990));
		
		ResponseTime third = new ResponseTime();
		third.setRequestTime(new Date().getTime() + 1500);
		third.setOperation("buy");
		third.setResponseTime(Long.valueOf(790));
		
		ResponseTime fourth = new ResponseTime();
		fourth.setRequestTime(new Date().getTime() + 1500);
		fourth.setOperation("checkCredit");
		fourth.setResponseTime(Long.valueOf(890));
		
		ResponseTime fiveth = new ResponseTime();
		fiveth.setRequestTime(new Date().getTime() + 1500);
		fiveth.setOperation("deliver");
		fiveth.setResponseTime(Long.valueOf(290));
		
		ResponseTime sixth = new ResponseTime();
		sixth.setRequestTime(new Date().getTime() + 1500);
		sixth.setOperation("buy");
		sixth.setResponseTime(Long.valueOf(1090));
		
		ResponseTime[] result = new ResponseTime[]{first, second, third, fourth, fiveth, sixth};
		return result;
	}

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

}
