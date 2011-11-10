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
import org.savara.sam.conversation.ConversationDetails;

@WebServlet("/Main")
public class AQMonitorServlet extends HttpServlet {

	private static final long serialVersionUID = -5486684388851115619L;

	static String PAGE_HEADER = "<html><head /><body>";
	static String PAGE_FOOTER = "</body></html>";

	//@javax.inject.Inject
	ActiveQueryManager _activeQueryManager;
	
	private ActiveQuery<ActivityAnalysis> _purchasingResponseTime;
	private ActiveListener<ActivityAnalysis> _purchasingResponseTimeListener;
	
	private ActiveQuery<ActivityAnalysis> _slaWarnings;
	private ActiveListener<ActivityAnalysis> _slaWarningsListener;
	
	private ActiveQuery<ActivitySummary> _startedTxns;
	private ActiveQuery<ActivitySummary> _completedTxns;
	private ActiveQuery<ActivitySummary> _failedTxns;
	private ActiveListener<ActivitySummary> _txnRatioListener;
	
	private ActiveQuery<ConversationDetails> _purchasingConversation;
	private ActiveListener<ConversationDetails> _purchasingConversationListener;
	
	private StringBuffer _responseTimeReport=new StringBuffer();
	private StringBuffer _txnRatioReport=new StringBuffer();
	private StringBuffer _slaWarningsReport=new StringBuffer();
	private StringBuffer _purchasingConversationReport=new StringBuffer();
	
	public void init() {
		// Alternative means of retrieving the active query manager, if injection cannot be used
		_activeQueryManager = org.savara.sam.aq.server.ActiveQueryServer.getInstance();

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
		
		_purchasingConversation = _activeQueryManager.getActiveQuery("PurchasingConversation");
		_purchasingConversationListener = new PurchasingConversationNotifier();
		_purchasingConversation.addActiveListener(_purchasingConversationListener);
		
	}

	@Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      PrintWriter writer = resp.getWriter();
      writer.println(PAGE_HEADER);
      writer.println("<h1>" + "SAVARA SAM Active Query Monitor" + "</h1>");
      writer.println(_txnRatioReport.toString());
      writer.println(_slaWarningsReport.toString());
      writer.println(_responseTimeReport.toString());
      writer.println(_purchasingConversationReport.toString());
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
		public void valueUpdated(ActivitySummary value) {
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
			_responseTimeReport = new StringBuffer();
			
			_responseTimeReport.append("<h3>Response Time Report ("+new java.util.Date()+")</h3>");
			
			for (ActivityAnalysis aa : _purchasingResponseTime.getContents()) {
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
		public void valueUpdated(ActivityAnalysis value) {
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
			_slaWarningsReport = new StringBuffer();
			
			_slaWarningsReport.append("<h3>SLA Warnings Report ("+new java.util.Date()+")</h3>");
			
			for (ActivityAnalysis aa : _slaWarnings.getContents()) {
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
		public void valueUpdated(ActivityAnalysis value) {
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
	
	public class PurchasingConversationNotifier implements ActiveListener<ConversationDetails> {

		public PurchasingConversationNotifier() {
			buildReport();
		}
		
		protected void buildReport() {
			_purchasingConversationReport = new StringBuffer();
			
			_purchasingConversationReport.append("<h3>Purchasing Conversation Report ("+new java.util.Date()+")</h3>");
			
			for (ConversationDetails cd : _purchasingConversation.getContents()) {
				_purchasingConversationReport.append("<h5>"+cd+"</h5>");
			}
		}
		
		@Override
		public void valueAdded(ConversationDetails value) {
			buildReport();
		}

		@Override
		public void valueUpdated(ConversationDetails value) {
			buildReport();
		}

		@Override
		public void valueRemoved(ConversationDetails value) {
			buildReport();
		}		

		@Override
		public void refresh() {
			buildReport();
		}		
	}

}
