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
package org.savara.sam.activity;

import org.savara.sam.activity.ServiceModel.ServiceInvocation;
import org.savara.sam.activity.ServiceModel.ServiceInvocation.InvocationType;
import org.savara.sam.activity.ActivityModel.Activity;

/**
 * This class represents the summary information for an activity
 * event.
 *
 */
public class ActivitySummary implements java.io.Serializable {

	private static final long serialVersionUID = -2567012277812787986L;

	private String _id=null;
	private long _timestamp=0;
	private ServiceInvocationSummary _serviceInvocation=null;
	
	public ActivitySummary(String id, Activity activity) {
		_id = id;
		
		initialize(activity);
	}
	
	protected void initialize(Activity activity) {
		_timestamp = activity.getTimestamp();
		
		if (activity.getServiceInvocation() != null) {
			_serviceInvocation = new ServiceInvocationSummary(activity.getServiceInvocation());
		}
	}
	
	public String getId() {
		return (_id);
	}
	
	public long getTimestamp() {
		return (_timestamp);
	}
	
	public ServiceInvocationSummary getServiceInvocation() {
		return (_serviceInvocation);
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ActivitySummary) {
			return (((ActivitySummary)obj)._id.equals(_id));
		}
		return false;
	}
	
	public int hashCode() {
		return (_id.hashCode());
	}
	
	public String toString() {
		return("Activity["+_id+"/"+_timestamp+"] si="+_serviceInvocation);
	}
	
	public class ServiceInvocationSummary implements java.io.Serializable {

		private static final long serialVersionUID = 6289313307914140423L;

		private String _operation=null;
		private String _serviceType=null;
		private String _fault=null;
		private boolean _request=false;
		
		public ServiceInvocationSummary(ServiceInvocation si) {
			initialize(si);
		}
		
		protected void initialize(ServiceInvocation si) {
			_operation = si.getOperation();
			_serviceType = si.getServiceType();
			_fault = si.getFault();
			_request = (si.getInvocationType() == InvocationType.REQUEST);
		}
		
		public String getOperation() {
			return (_operation);
		}
		
		public String getFault() {
			return (_fault);
		}
		
		public String getServiceType() {
			return (_serviceType);
		}
		
		public boolean isRequest() {
			return (_request);
		}
		
		public String toString() {
			return("{serviceType="+_serviceType+" op="+_operation+" fault="+_fault+" request="+_request+"}");
		}
	}
}
