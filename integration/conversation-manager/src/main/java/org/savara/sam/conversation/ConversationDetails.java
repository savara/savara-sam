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
package org.savara.sam.conversation;

import org.savara.monitor.ConversationId;
import org.savara.monitor.MonitorResult;
import org.savara.sam.activity.ActivitySummary;

/**
 * This class represents the activity information associated with a
 * particular conversation instance.
 *
 */
public class ConversationDetails implements java.io.Serializable {
	
	private static final long serialVersionUID = 7878002530703171118L;

	private ConversationId _id=null;
	private boolean _valid=true;
	private java.util.List<ActivityResultDetails> _details=new java.util.ArrayList<ActivityResultDetails>();
	private long _startTimestamp=0;
	private long _endTimestamp=0;
	
	/**
	 * This is the constructor for the conversation details.
	 * 
	 * @param id The conversation id
	 */
	public ConversationDetails(ConversationId id) {
		_id = id;
	}
	
	/**
	 * This method returns the conversation id associated with the
	 * details.
	 * 
	 * @return The conversation id
	 */
	public ConversationId getId() {
		return(_id);
	}
	
	/**
	 * This method determines if the conversation instance is valid,
	 * in respect of the protocol against which it was monitored.
	 * 
	 * @return Whether the conversation is valid
	 */
	public boolean isValid() {
		return(_valid);
	}
	
	/**
	 * This method adds an activity summary to the details associated with a conversation
	 * id.
	 * 
	 * @param activity The activity
	 * @param result The optional monitoring result
	 */
	public void addActivity(ActivitySummary activity, MonitorResult result) {
		
		if (_startTimestamp == 0) {
			_startTimestamp = activity.getTimestamp();
		}
		
		_endTimestamp = activity.getTimestamp();
		
		_details.add(new ActivityResultDetails(activity, result));
		
		if (result != null && !result.isValid()) {
			_valid = false;
		}
	}
	
	public long getStartTimestamp() {
		return(_startTimestamp);
	}
	
	public long getEndTimestamp() {
		return(_endTimestamp);
	}
	
	public String toString() {
		return("Conversation "+_id+" valid="+_valid+" start="+_startTimestamp+
				" end="+_endTimestamp+" details="+_details);
	}
	
	public int hashCode() {
		return(_id.hashCode());
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ConversationDetails) {
			return (((ConversationDetails)obj)._id.equals(_id));
		}
		return(false);
	}
	
	/**
	 * This class provides a container for the activity summary and monitoring results
	 * information.
	 *
	 */
	protected static class ActivityResultDetails implements java.io.Serializable {
		
		private static final long serialVersionUID = -8998387654349909939L;

		private ActivitySummary _activity=null;
		private MonitorResult _result=null;
		
		/**
		 * The constructor.
		 * 
		 * @param activity The activity
		 * @param result The monitor result
		 */
		public ActivityResultDetails(ActivitySummary activity, MonitorResult result) {
			_activity = activity;
			_result = result;
		}
		
		/**
		 * This method returns the activity.
		 * 
		 * @return The activity
		 */
		public ActivitySummary getActivity() {
			return(_activity);
		}
		
		/**
		 * This method returns the monitor result.
		 * 
		 * @return The monitor result
		 */
		public MonitorResult getResult() {
			return(_result);
		}
		
		public String toString() {
			return("Activity="+_activity+" Result="+_result);
		}
	}
}
