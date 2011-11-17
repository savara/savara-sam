/**
 * 
 */
package org.savara.sam.web.shared;

import java.util.List;
import java.util.Map;

import org.savara.sam.web.shared.dto.AQChartModel;
import org.savara.sam.web.shared.dto.Conversation;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * @author Jeff Yu
 * @date Nov 4, 2011
 */
@RemoteServiceRelativePath("AQMoniterService")
public interface AQMonitorService extends RemoteService{
	
	public List<String> getSystemAQNames();
	
	public Map getChartData(AQChartModel model);
		
	public Conversation[] getConversationDetails();
	
}
