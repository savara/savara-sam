/**
 * 
 */
package org.savara.sam.web.shared;

import org.savara.sam.web.shared.dto.Conversation;
import org.savara.sam.web.shared.dto.ResponseTime;
import org.savara.sam.web.shared.dto.Statistic;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * @author Jeff Yu
 * @date Nov 4, 2011
 */
@RemoteServiceRelativePath("AQMoniterService")
public interface AQMonitorService extends RemoteService{
	
	public Statistic[] getStatistics();
	
	public ResponseTime[] getResponseTimes();
	
	public Conversation[] getConversationDetails();
	
}
