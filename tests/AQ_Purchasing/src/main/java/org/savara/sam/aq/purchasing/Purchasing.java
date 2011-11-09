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
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.aq.Predicate;
import org.savara.sam.aq.server.JEEActiveQueryManager;

@MessageDriven(name = "Purchasing", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/Purchasing")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class Purchasing extends JEEActiveQueryManager<ActivitySummary,ActivitySummary> implements MessageListener {
	
	private static final String ACTIVE_QUERY_NAME = "Purchasing";
	private static final String PARENT_ACTIVE_QUERY_NAME = "Root";

	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
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
		super(ACTIVE_QUERY_NAME, PARENT_ACTIVE_QUERY_NAME);
	}
	
	@PostConstruct
	public void init() {
		super.init(_connectionFactory, _container, _notificationTopic, _purchasingStarted, _purchasingSuccessful,
				_purchasingUnsuccessful, _purchasingResponseTime, _purchasingConversation);
	}

	@PreDestroy
	public void close() {
		super.close();
	}
	
	@Override
	protected Predicate<ActivitySummary> getPredicate() {
		return(new PurchasingPredicate());
	}
	
	public static class PurchasingPredicate implements Predicate<ActivitySummary>, java.io.Serializable {
		
		private static final long serialVersionUID = -2369012295880166599L;
		
		private static final java.util.List<String> SERVICE_TYPES=new java.util.Vector<String>();
		
		static {
			SERVICE_TYPES.add("{http://www.jboss.org/examples/store}Store");
			SERVICE_TYPES.add("{http://www.jboss.org/examples/creditAgency}CreditAgency");
			SERVICE_TYPES.add("{http://www.jboss.org/examples/logistics}Logistics");
		}

		public PurchasingPredicate() {
		}

		public boolean evaluate(ActivitySummary value) {
			
			if (value.getServiceInvocation() != null &&
					SERVICE_TYPES.contains(value.getServiceInvocation().getServiceType())) {
				return (true);
			}

			return false;
		}
	}
}
