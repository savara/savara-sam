/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008-11, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.savara.sam.tests.aq.monitor;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.aq.ActiveListener;
import org.savara.sam.aq.ActiveQuery;
import org.savara.sam.aq.ActiveQueryManager;
import org.savara.sam.aq.Predicate;

@SuppressWarnings("serial")
@WebServlet("/Main")
@ApplicationScoped
public class AQMonitorServlet extends HttpServlet {

   static String PAGE_HEADER = "<html><head /><body>";

   static String PAGE_FOOTER = "</body></html>";

	@Inject
	ActiveQueryManager _activeQueryManager;
	
	private ActiveQuery<ActivityAnalysis> _purchasingResponseTime;
	private ActiveListener<ActivityAnalysis> _purchasingResponseTimeListener;
	
	private ActiveQuery<ActivityAnalysis> _slaWarnings;
	private ActiveListener<ActivityAnalysis> _slaWarningsListener;
	
	private ActiveQuery<ActivitySummary> _startedTxns;
	private ActiveQuery<ActivitySummary> _completedTxns;
	private ActiveQuery<ActivitySummary> _failedTxns;
	private ActiveListener<ActivitySummary> _txnRatioListener;
	
	private StringBuffer _responseTimeReport=new StringBuffer();
	private StringBuffer _txnRatioReport=new StringBuffer();
	private StringBuffer _slaWarningsReport=new StringBuffer();
	
	private boolean _initialized=false;
	
	@PostConstruct
	public void init() {
				
		// Appears that init is being called twice????
		if (!_initialized) {
		
			_startedTxns = _activeQueryManager.getActiveQuery("PurchasingStarted");
			_completedTxns = _activeQueryManager.getActiveQuery("PurchasingSuccessful");
			_failedTxns = _activeQueryManager.getActiveQuery("PurchasingUnsuccessful");
			_txnRatioListener = new TxnRatioNotifier();
			_startedTxns.addActiveListener(_txnRatioListener);
			_completedTxns.addActiveListener(_txnRatioListener);
			_failedTxns.addActiveListener(_txnRatioListener);

			_purchasingResponseTime = _activeQueryManager.getActiveQuery("PurchasingResponseTime");
			_purchasingResponseTimeListener = new ResponseTimeNotifier();
			_purchasingResponseTime.addActiveListener(_purchasingResponseTimeListener);
			
			_slaWarnings = _activeQueryManager.createActiveQuery(_purchasingResponseTime,
								new Predicate<ActivityAnalysis>() {
				public boolean evaluate(ActivityAnalysis value) {
					long responseTime=(Long)value.getProperty("responseTime").getValue();
					return responseTime > 9000;
				}
			});
			_slaWarningsListener = new SLAWarningsNotifier();
			_slaWarnings.addActiveListener(_slaWarningsListener);
		}
	}

	@PreDestroy
	public void close() {
		_initialized = false;
	}

	@Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      PrintWriter writer = resp.getWriter();
      writer.println(PAGE_HEADER);
      writer.println("<h1>" + "SAVARA SAM Active Query Monitor" + "</h1>");
      writer.println(_txnRatioReport.toString());
      writer.println(_slaWarningsReport.toString());
      writer.println(_responseTimeReport.toString());
      writer.println(PAGE_FOOTER);
      writer.close();
   }

	public class TxnRatioNotifier implements ActiveListener<ActivitySummary> {

		public TxnRatioNotifier() {
			buildReport();
		}
		
		protected void buildReport() {
			_txnRatioReport = new StringBuffer();
			
			_txnRatioReport.append("<h3>Transaction Ratio Report ("+new java.util.Date()+")</h3>");
			
			_txnRatioReport.append("<h5>Started "+_startedTxns.size()+" : Successful "+
						_completedTxns.size()+" : Unsuccessful "+_failedTxns.size()+"</h5>");
		}
		
		@Override
		public void valueAdded(ActivitySummary value) {
			buildReport();
		}

		@Override
		public void valueRemoved(ActivitySummary value) {
			buildReport();
		}		

		@Override
		public void refresh() {
			buildReport();
		}		
	}

	public class ResponseTimeNotifier implements ActiveListener<ActivityAnalysis> {

		public ResponseTimeNotifier() {
			buildReport();
		}
		
		protected void buildReport() {
			java.util.Iterator<ActivityAnalysis> iter=_purchasingResponseTime.getResults();
			_responseTimeReport = new StringBuffer();
			
			_responseTimeReport.append("<h3>Response Time Report ("+new java.util.Date()+")</h3>");
			
			while (iter.hasNext()) {
				ActivityAnalysis aa=iter.next();
				String operation=(String)aa.getProperty("operation").getValue();
				long responseTime=(Long)aa.getProperty("responseTime").getValue();
				_responseTimeReport.append("<h5>Operation "+operation+" : response time "+responseTime+"ms</h5>");
			}
		}
		
		@Override
		public void valueAdded(ActivityAnalysis value) {
			buildReport();
		}

		@Override
		public void valueRemoved(ActivityAnalysis value) {
			buildReport();
		}		

		@Override
		public void refresh() {
			buildReport();
		}		
	}

	public class SLAWarningsNotifier implements ActiveListener<ActivityAnalysis> {

		public SLAWarningsNotifier() {
			buildReport();
		}
		
		protected void buildReport() {
			java.util.Iterator<ActivityAnalysis> iter=_slaWarnings.getResults();
			_slaWarningsReport = new StringBuffer();
			
			_slaWarningsReport.append("<h3>SLA Warnings Report ("+new java.util.Date()+")</h3>");
			
			while (iter.hasNext()) {
				ActivityAnalysis aa=iter.next();
				String principal=(String)aa.getProperty("principal").getValue();
				long responseTime=(Long)aa.getProperty("responseTime").getValue();
				_slaWarningsReport.append("<h5>Principal "+principal+" : response time "+responseTime+"ms</h5>");
			}
		}
		
		@Override
		public void valueAdded(ActivityAnalysis value) {
			buildReport();
		}

		@Override
		public void valueRemoved(ActivityAnalysis value) {
			buildReport();
		}		

		@Override
		public void refresh() {
			buildReport();
		}		
	}
}
