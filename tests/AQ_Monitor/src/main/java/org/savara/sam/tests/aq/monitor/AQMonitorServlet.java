package org.savara.sam.tests.aq.monitor;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
//import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.aq.ActiveListener;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.server.ActiveQueryServer;

@SuppressWarnings("serial")
@WebServlet("/Main")
@ApplicationScoped
public class AQMonitorServlet extends HttpServlet {

   static String PAGE_HEADER = "<html><head /><body>";

   static String PAGE_FOOTER = "</body></html>";

	//@Resource(mappedName = "java:/JmsXA")
	//ConnectionFactory _connectionFactory;
	
	//@Inject
	ActiveQueryManager _activeQueryManager;
	
	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	//private org.infinispan.Cache<String, DefaultActiveQuery<ActivitySummary>> _cache;

	private ActiveQuery<ActivityAnalysis> _purchasingResponseTime;
	private ActiveListener<ActivityAnalysis> _purchasingResponseTimeListener;
	
	private StringBuffer _report=new StringBuffer();
	
	// NOTES:
	// Need to see whether cache update notification should be used? But then won't be
	// same - would need to see what form the notification takes, and whether could be
	// translated into current notification form.
	// Otherwise need to find way for 'active query proxy' to get notifications. Possibly
	// each AQ needs to provide a topic based MDB that the AQProxy can hook into?
	
	
	@PostConstruct
	public void init() {
		
		// Appears that init is being called twice????
		if (_activeQueryManager == null) {
		
			// TODO: Should be via injection, but does not seem to work across deployments, even
			// when dependency setup in the manifest - to be investigated further
			_activeQueryManager = new ActiveQueryServer(_container);
			
			_purchasingResponseTime = _activeQueryManager.getActiveQuery("PurchasingResponseTime");
			_purchasingResponseTimeListener = new ActiveQueryNotifier<ActivityAnalysis>("PurchasingResponseTime");
			_purchasingResponseTime.addActiveListener(_purchasingResponseTimeListener);
		}
	}

	@PreDestroy
	public void close() {
	}

	@Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      PrintWriter writer = resp.getWriter();
      writer.println(PAGE_HEADER);
      writer.println("<h1>" + "SAVARA SAM Active Query Monitor" + "</h1>");
      writer.println(_report.toString());
      writer.println(PAGE_FOOTER);
      writer.close();
   }

	public class ActiveQueryNotifier<T> implements ActiveListener<T> {

		private String _name;
		
		public ActiveQueryNotifier(String aqname) {
			_name = aqname;
		}
		
		@Override
		public void valueAdded(T value) {
			_report.append("<h3>"+_name+" : "+value.toString()+"</h3>");
		}

		@Override
		public void valueRemoved(T value) {
		}		
	}
}
