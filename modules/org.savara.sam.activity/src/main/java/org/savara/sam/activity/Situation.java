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

import java.util.UUID;

/**
 * This class represents a situation of information relevant to
 * users of the Service Activity Monitoring capability.
 *
 */
public class Situation implements java.io.Serializable {

	private static final long serialVersionUID = 8241317557052964415L;

	private String _id=UUID.randomUUID().toString();
	
	private long _createdTimestamp=System.currentTimeMillis();
	
	private Severity _severity=Severity.Minor;
	private Priority _priority=Priority.Low;
	private Status _status=Status.New;
	
	private String _principal=null;
	private String _description=null;
	private String _externalReference=null;
	
	private String _owner=null;
	
	private java.util.Map<String,String> _properties=new java.util.HashMap<String,String>();
	
	private java.util.List<ChangeHistory> _changeHistory=new java.util.Vector<ChangeHistory>();
	
	/**
	 * Constructor for the situation.
	 * 
	 * @param principal The optional principal associated with the situation being
	 * 						notified
	 * @param description The description
	 * @param severity The severity
	 * @param priority The priority
	 */
	public Situation(String principal, String description, Severity severity, Priority priority) {
		_principal = principal;
		_description = description;
		_severity = severity;
		_priority = priority;
	}
	
	/**
	 * This method returns the id for the situation.
	 * 
	 * @return The id
	 */
	public String getId() {
		return(_id);
	}
	
	/**
	 * This method returns the timestamp when the situation was created.
	 * 
	 * @return The creation timestamp
	 */
	public long getCreatedTimestamp() {
		return(_createdTimestamp);
	}
	
	/**
	 * This method returns the timestamp when the situation was last
	 * updated.
	 * 
	 * @return The update timestamp
	 */
	public long getLastUpdateTimestamp() {
		if (_changeHistory.size() > 0) {
			return(_changeHistory.get(_changeHistory.size()-1).getTimestamp());
		}
		return(_createdTimestamp);
	}
	
	/**
	 * This method returns the principal associated with the situation.
	 * 
	 * @return The principal associated with the situation
	 */
	public String getPrincipal() {
		return(_principal);
	}
	
	/**
	 * This method returns the description associated with the situation.
	 * 
	 * @return The description associated with the situation
	 */
	public String getDescription() {
		return(_description);
	}
	
	/**
	 * This method returns the current owner.
	 * 
	 * @return The current owner
	 */
	public String getOwner() {
		return(_owner);
	}
	
	/**
	 * This method returns the severity.
	 * 
	 * @return The severity
	 */
	public Severity getSeverity() {
		return(_severity);
	}
	
	/**
	 * This method returns the priority.
	 * 
	 * @return The priority
	 */
	public Priority getPriority() {
		return(_priority);
	}
	
	/**
	 * This method returns the optional external reference,
	 * used when the situation has been recorded in an external
	 * system.
	 * 
	 * @return The optional external reference
	 */
	public String getExternalReference() {
		return(_externalReference);
	}
	
	/**
	 * This method returns the status.
	 * 
	 * @return The status
	 */
	public Status getStatus() {
		return(_status);
	}
	
	/**
	 * This method returns the property value.
	 * 
	 * @param name The name of the required property
	 * @return The value, or null if not found
	 */
	public String getProperty(String name) {
		return(_properties.get(name));
	}
	
	/**
	 * This method sets a property.
	 * 
	 * @param person The person performing the task
	 * @param name The name of the property
	 * @param value The value of the property
	 */
	public void setProperty(String person, String name, String value) {
		recordChange(person, "Set property '"+name+"' to '"+value+"'");
		_properties.put(name, value);
	}
	
	/**
	 * This method returns a copy of the properties associated with the
	 * situation, therefore any changes to the returned map will have
	 * no long term effect on the situation.
	 * 
	 * @return The properties
	 */
	public java.util.Map<String,String> getProperties() {
		return(new java.util.HashMap<String,String>(_properties));
	}
	
	protected void recordChange(String person, String change) {
		_changeHistory.add(new ChangeHistory(person, change));
	}
	
	/**
	 * This method sets the external reference associated with the situation.
	 * 
	 * @param person The person performing the task
	 * @param externalRef The external reference
	 */
	public void assignExternalReference(String person, String externalRef) {
		recordChange(person, "Changed external reference from '"+
						_externalReference+"' to '"+externalRef+"'");
		_externalReference = externalRef;
	}
	
	public void takeOwnership(String owner) {
		recordChange(owner, "Changed owner from '"+_owner+"' to '"+owner+"'");
		_owner = owner;
	}
	
	/**
	 * This method changes the status associated with the situation.
	 * 
	 * @param person The person performing the task
	 * @param status The status
	 * @param reason The reason
	 */
	public void changeStatus(String person, Status status, String reason) {
		recordChange(person, "Changed status from '"+_status+"' to '"+status+"'"+
					(reason==null ? "" : " reason '"+reason+"'"));
		_status = status;
	}
	
	/**
	 * This method changes the priority associated with the situation.
	 * 
	 * @param person The person performing the task
	 * @param priority The priority
	 * @param reason The reason
	 */
	public void changePriority(String person, Priority priority, String reason) {
		recordChange(person, "Changed priority from '"+priority+"' to '"+priority+"'"+
					(reason==null ? "" : " reason '"+reason+"'"));
		_priority = priority;
	}
	
	/**
	 * This method changes the severity associated with the situation.
	 * 
	 * @param person The person performing the task
	 * @param severity The severity
	 * @param reason The reason
	 */
	public void changeSeverity(String person, Severity severity, String reason) {
		recordChange(person, "Changed severity from '"+_severity+"' to '"+severity+"'"+
					(reason==null ? "" : " reason '"+reason+"'"));
		_severity = severity;
	}
	
	public java.util.List<ChangeHistory> getChangeHistory() {
		// Return a copy
		return (new java.util.Vector<ChangeHistory>(_changeHistory));
	}
	
	public int hashCode() {
		return(_id.hashCode());
	}
	
	public boolean equals(Object obj) {
		boolean ret=false;
		
		if (obj instanceof Situation) {
			ret = ((Situation)obj)._id.equals(_id);
		}
		
		return(ret);
	}
	
	public String toString() {
		return("Situation[id="+_id+" principal="+_principal+" "+_status+" "+
					_severity+" "+_priority+" '"+_description+"' owner="+_owner+
					" props="+_properties+"]");
	}
	
	/**
	 * The status.
	 */
	public enum Status {
		New,
		Accepted,
		Resolved,
		Rejected
	}
	
	/**
	 * Severity.
	 */
	public enum Severity {
		Minor,
		Major,
		Critical
	}
	
	/**
	 * Priority.
	 */
	public enum Priority {
		Low,
		Medium,
		High
	}
	
	/**
	 * Class representing change history entries.
	 */
	public static class ChangeHistory implements java.io.Serializable {

		private static final long serialVersionUID = -3511700020276598245L;
	
		private String _person;
		private String _description;
		private long _timestamp;
		
		/**
		 * Constructor for the change history entry.
		 * 
		 * @param person The person submitting the change
		 * @param description The description of the change
		 */
		public ChangeHistory(String person, String description) {
			_person = person;
			_description = description;
			_timestamp = System.currentTimeMillis();
		}
		
		/**
		 * This method returns the name of the person who has made
		 * the change.
		 * 
		 * @return The name of the person who made the change
		 */
		public String getPerson() {
			return(_person);
		}
		
		/**
		 * This method returns the description of the change.
		 * 
		 * @return The descripton of the change
		 */
		public String getDescription() {
			return(_description);
		}
		
		/**
		 * This method returns the timestamp of the change.
		 * 
		 * @return The timestamp of the change
		 */
		public long getTimestamp() {
			return(_timestamp);
		}
	}
}
