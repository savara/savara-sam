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
package org.savara.sam.aq.purchasingsuccessful;

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
import org.savara.sam.activity.ServiceModel.ServiceInvocation;
import org.savara.sam.aq.ActiveQuerySpec;
import org.savara.sam.aq.Predicate;
import org.savara.sam.aq.server.ActiveQueryServer;
import org.savara.sam.aq.server.JEEActiveQueryManager;
import org.savara.sam.aq.server.JEECacheActiveQuerySpec;

@MessageDriven(name = "PurchasingSuccessful", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/PurchasingSuccessful")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class PurchasingSuccessful extends JEEActiveQueryManager<String,String> implements MessageListener {
	
	private static final String CACHE_NAME = "activities";
	private static final String ACTIVE_QUERY_NAME = "PurchasingSuccessful";
	private static final String PARENT_ACTIVE_QUERY_NAME = "Purchasing";

	//@Resource(mappedName = "java:/JmsXA")
	//ConnectionFactory _connectionFactory;
	
	@Resource(mappedName = "java:/queue/aq/PurchasingSuccessful")
	Destination _sourceQueue;
	
	@Resource(mappedName = "java:/topic/aq/Notifications")
	Destination _notificationTopic;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;
	
	public PurchasingSuccessful() {
		super(new JEECacheActiveQuerySpec(ACTIVE_QUERY_NAME, Activity.class, String.class),
				PARENT_ACTIVE_QUERY_NAME);
	}
	
	@PostConstruct
	public void init() {
		super.init(null, _container, _sourceQueue, _notificationTopic);
		
		((JEECacheActiveQuerySpec)getActiveQuerySpec()).setCache(_container.getCache(CACHE_NAME));
	}

	@PreDestroy
	public void close() {
		super.close();
	}
	
	@Override
	protected Predicate<String> getPredicate() {
		return(new PurchasingSuccessfulPredicate());
	}
	
	public static class PurchasingSuccessfulPredicate implements Predicate<String>, java.io.Serializable {
		
		private static final long serialVersionUID = 5086630412993298230L;

		private static ActiveQuerySpec _aqSpec=null;

		public PurchasingSuccessfulPredicate() {
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

			ServiceInvocation si=activity.getServiceInvocation();
			
			if (si != null &&
					si.getServiceType().equals("{http://www.jboss.org/examples/store}Store") &&
					si.getOperation().equals("buy") &&
					si.getInvocationType() != ServiceInvocation.InvocationType.REQUEST &&
					si.getDirection() == ServiceInvocation.Direction.OUTBOUND &&
					(si.getFault() == null || si.getFault().trim().length() == 0)) {
				return (true);
			}

			return false;
		}
	}
}
