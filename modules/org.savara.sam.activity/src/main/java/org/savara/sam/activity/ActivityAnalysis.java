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

/**
 * This class represents the result of the analysis of one or more
 * activity events.
 *
 */
public class ActivityAnalysis implements java.io.Serializable {

	private static final long serialVersionUID = 2226789381266548746L;
	
	private java.util.Map<String, Property> _properties=new java.util.HashMap<String, ActivityAnalysis.Property>();
	
	public ActivityAnalysis() {
	}
	
	public void addProperty(Property prop) {
		_properties.put(prop.getName(), prop);
	}
	
	public void addProperty(String name, String type, java.io.Serializable value) {
		_properties.put(name, new Property(name, type, value));
	}
	
	public void removeProperty(Property prop) {
		_properties.remove(prop.getName());
	}
	
	public Property getProperty(String name) {
		return(_properties.get(name));
	}
	
	public String toString() {
		return(_properties.toString());
	}

	public static class Property implements java.io.Serializable {

		private static final long serialVersionUID = -1306072811132928093L;
	
		private String _name=null;
		private String _type=null;
		private java.io.Serializable _value=null;
		
		public Property(String name, String type, java.io.Serializable value) {
			_name = name;
			_type = type;
			_value = value;
		}
		
		public String getName() {
			return (_name);
		}
		
		public String getType() {
			return (_type);
		}
		
		public java.io.Serializable getValue() {
			return (_value);
		}
		
		public String toString() {
			return("Property{"+_name+","+_type+","+_value+"}");
		}
	}
}
