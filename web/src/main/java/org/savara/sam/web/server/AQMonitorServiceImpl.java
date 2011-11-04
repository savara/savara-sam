/**
 * 
 */
package org.savara.sam.web.server;

import org.savara.sam.web.shared.AQMonitorService;
import org.savara.sam.web.shared.dto.Statistic;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * @author Jeff Yu
 * @date Nov 4, 2011
 */

public class AQMonitorServiceImpl extends RemoteServiceServlet implements AQMonitorService {
	
	private static final long serialVersionUID = 8965645007479773817L;

	
	public Statistic[] getStatistics() {

		Statistic s1 = new Statistic();
		s1.setValue(20);
		s1.setName("Started");
		
		Statistic s2 = new Statistic();
		s2.setValue(18);
		s2.setName("Successful");
		
		Statistic s3 = new Statistic();
		s3.setValue(2);
		s3.setName("Unsuccessful");
		
		 Statistic[] result = new Statistic[]{s1, s2, s3};
		 return result;
	}

}
