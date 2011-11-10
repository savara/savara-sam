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
package org.savara.sam.aq.purchasingresponsetime;

import java.util.concurrent.TimeUnit;

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

import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.activity.ActivitySummary;
import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.server.JEEActiveQueryManager;

@MessageDriven(name = "PurchasingResponseTime", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/PurchasingResponseTime")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class PurchasingResponseTime extends JEEActiveQueryManager<ActivitySummary,ActivityAnalysis> implements MessageListener {
	
	private static final String ACTIVE_QUERY_NAME = "PurchasingResponseTime";

	@Resource(mappedName = "java:/JmsXA")
	ConnectionFactory _connectionFactory;
	
	@Resource(mappedName = "java:/topic/aq/Notifications")
	Destination _notificationTopic;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;

	private org.infinispan.Cache<String, ActivitySummary> _siCache;
	
	// TODO: Determine how best to handle this type of active query, that has
	// multiple parent AQs???
	
	public PurchasingResponseTime() {
		super(ACTIVE_QUERY_NAME, null);
	}
	
	@PostConstruct
	public void init() {
		super.init(_connectionFactory, _container, _notificationTopic);

		_siCache = _container.getCache("serviceInvocations");
	}

	@PreDestroy
	public void close() {
		super.close();
	}

	@Override
	protected ActivityAnalysis processActivity(ActivitySummary activity, ActiveChangeType changeType) {
		ActivityAnalysis ret=null;
		
		// Check if service interaction with correlation
		if (activity.getServiceInvocation() != null &&
				activity.getServiceInvocation().getCorrelation() != null) {
			String correlation=activity.getServiceInvocation().getCorrelation();
			
			// Check if correlated invocation already exists
			ActivitySummary other=_siCache.get(correlation);
			
			if (other == null) {
				_siCache.put(correlation, activity, 150, TimeUnit.SECONDS);
			} else {
				// Create activity results object for correlated match
				ret = new ActivityAnalysis();
				
				long responseTime=activity.getTimestamp()-other.getTimestamp();
				
				ret.addProperty("requestTimestamp", Long.class.getName(), other.getTimestamp());
				ret.addProperty("requestId", String.class.getName(), other.getId());
				ret.addProperty("responseId", String.class.getName(), activity.getId());
				ret.addProperty("responseTime", Long.class.getName(), responseTime);
				ret.addProperty("serviceType", String.class.getName(), activity.getServiceInvocation().getServiceType());
				ret.addProperty("operation", String.class.getName(), activity.getServiceInvocation().getOperation());
				ret.addProperty("fault", String.class.getName(), activity.getServiceInvocation().getFault());
				ret.addProperty("principal", String.class.getName(), activity.getPrincipal());
			}
		}
		
		return(ret);
	}
}
