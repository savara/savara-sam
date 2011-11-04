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
package org.savara.sam.aq.purchasingunsuccessful;

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
import org.savara.sam.activity.ActivitySummary.ServiceInvocationSummary;
import org.savara.sam.aq.Predicate;
import org.savara.sam.aq.server.JEEActiveQueryManager;

@MessageDriven(name = "PurchasingUnsuccessful", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/PurchasingUnsuccessful")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class PurchasingUnsuccessful extends JEEActiveQueryManager<ActivitySummary,ActivitySummary> implements MessageListener {
	
	private static final String ACTIVE_QUERY_NAME = "PurchasingUnsuccessful";
	private static final String PARENT_ACTIVE_QUERY_NAME = "Purchasing";

	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
	@Resource(mappedName = "java:/topic/aq/Notifications")
	Destination _notificationTopic;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	
	public PurchasingUnsuccessful() {
		super(ACTIVE_QUERY_NAME, PARENT_ACTIVE_QUERY_NAME);
	}
	
	@PostConstruct
	public void init() {
		super.init(_connectionFactory, _container, _notificationTopic);
	}

	@PreDestroy
	public void close() {
		super.close();
	}
	
	@Override
	protected Predicate<ActivitySummary> getPredicate() {
		return(new PurchasingUnsuccessfulPredicate());
	}
	
	public static class PurchasingUnsuccessfulPredicate implements Predicate<ActivitySummary>, java.io.Serializable {
		
		private static final long serialVersionUID = 5086630412993298230L;

		public PurchasingUnsuccessfulPredicate() {
		}

		public boolean evaluate(ActivitySummary value) {
			ServiceInvocationSummary si=value.getServiceInvocation();
			
			if (si != null &&
					si.getServiceType().equals("Broker") &&
					si.getOperation().equals("buy") &&
					!si.isRequest() &&
					(si.getFault() != null && si.getFault().trim().length() > 0)) {
				return (true);
			}

			return false;
		}
	}
}
