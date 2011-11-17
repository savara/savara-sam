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
package org.savara.sam.aq.purchasing;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.savara.sam.activity.ActivityModel.Activity;
import org.savara.sam.aq.ActiveQuerySpec;
import org.savara.sam.aq.Predicate;
import org.savara.sam.aqs.ActiveQueryServer;
import org.savara.sam.aqs.JEEActiveQueryManager;
import org.savara.sam.aqs.JEECacheActiveQuerySpec;

@MessageDriven(name = "Purchasing", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/Purchasing")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class Purchasing extends JEEActiveQueryManager<String,String> implements MessageListener {
	
	private static final String CACHE_NAME = "activities";
	private static final String ACTIVE_QUERY_NAME = "Purchasing";
	private static final String PARENT_ACTIVE_QUERY_NAME = "Root";

	//@Resource(mappedName = "java:/JmsXA")
	//ConnectionFactory _connectionFactory;
	
	@Resource(mappedName = "java:/queue/aq/Purchasing")
	Destination _sourceQueue;
	
	@Resource(mappedName = "java:/queue/aq/PurchasingStarted")
	Destination _purchasingStarted;
	
	@Resource(mappedName = "java:/queue/aq/PurchasingSuccessful")
	Destination _purchasingSuccessful;
	
	@Resource(mappedName = "java:/queue/aq/PurchasingUnsuccessful")
	Destination _purchasingUnsuccessful;
	
	@Resource(mappedName = "java:/queue/aq/PurchasingResponseTime")
	Destination _purchasingResponseTime;
	
	@Resource(mappedName = "java:/queue/aq/PurchasingConversation")
	Destination _purchasingConversation;
	
	@Resource(mappedName = "java:/topic/aq/Notifications")
	Destination _notificationTopic;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	
	public Purchasing() {
		super(new JEECacheActiveQuerySpec(ACTIVE_QUERY_NAME, Activity.class, String.class),
						PARENT_ACTIVE_QUERY_NAME);
	}
	
	@PostConstruct
	public void init() {
		super.init(_container, _sourceQueue, _notificationTopic,
				_purchasingStarted, _purchasingSuccessful,
				_purchasingUnsuccessful, _purchasingResponseTime, _purchasingConversation);
		
		((JEECacheActiveQuerySpec)getActiveQuerySpec()).setCache(_container.getCache(CACHE_NAME));
	}

	@PreDestroy
	public void close() {
		super.close();
	}
	
	@Override
	protected Predicate<String> getPredicate() {
		return(new PurchasingPredicate());
	}
	
	public static class PurchasingPredicate implements Predicate<String>, java.io.Serializable {
		
		private static final long serialVersionUID = -2369012295880166599L;
		
		private static ActiveQuerySpec _aqSpec=null;
		
		private static final java.util.List<String> SERVICE_TYPES=new java.util.Vector<String>();
		
		static {
			SERVICE_TYPES.add("{http://www.jboss.org/examples/store}Store");
			SERVICE_TYPES.add("{http://www.jboss.org/examples/creditAgency}CreditAgency");
			SERVICE_TYPES.add("{http://www.jboss.org/examples/logistics}Logistics");
		}

		public PurchasingPredicate() {
		}

		public boolean evaluate(String value) {
			
			if (_aqSpec == null) {
				_aqSpec = ActiveQueryServer.getInstance().getActiveQuerySpec(ACTIVE_QUERY_NAME);
				
				if (_aqSpec == null) {
					throw new RuntimeException("Failed to get ActiveQuerySpec for:"+ACTIVE_QUERY_NAME);
				}
			}
			
			Activity activity=(Activity)_aqSpec.resolve(value);
			
			if (activity == null) {
				throw new RuntimeException("Failed to find acivity '"+value+
								"' for query '"+ACTIVE_QUERY_NAME+"'");
			}

			if (activity.getServiceInvocation() != null &&
					SERVICE_TYPES.contains(activity.getServiceInvocation().getServiceType())) {
				return (true);
			}

			return false;
		}
	}
}
