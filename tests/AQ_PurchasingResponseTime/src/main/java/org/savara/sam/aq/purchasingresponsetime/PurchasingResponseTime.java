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
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.savara.sam.activity.ActivityAnalysis;
import org.savara.sam.activity.ActivityModel.Activity;
import org.savara.sam.aq.ActiveChangeType;
import org.savara.sam.aq.ActiveQuerySpec;
import org.savara.sam.aqs.JEEActiveQueryManager;

@MessageDriven(name = "PurchasingResponseTime", messageListenerInterface = MessageListener.class,
               activationConfig =
                     {
                        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/aq/PurchasingResponseTime")
                     })
@TransactionManagement(value= TransactionManagementType.CONTAINER)
@TransactionAttribute(value= TransactionAttributeType.REQUIRED)
public class PurchasingResponseTime extends JEEActiveQueryManager<String,ActivityAnalysis> implements MessageListener {
	
	private static final String ACTIVE_QUERY_NAME = "PurchasingResponseTime";

	@Resource(mappedName = "java:/topic/aq/Notifications")
	Destination _notificationTopic;

	@Resource(mappedName = "java:/queue/aq/PurchasingResponseTime")
	Destination _sourceQueue;

	@Resource(mappedName="java:jboss/infinispan/sam")
	private org.infinispan.manager.CacheContainer _container;

	private org.infinispan.Cache<String, ActivityAnalysis> _siCache;
	private org.infinispan.Cache<String, Activity> _activitiesCache;
	
	// TODO: Determine how best to handle this type of active query, that has
	// multiple parent AQs???
	
	public PurchasingResponseTime() {
		super(new ActiveQuerySpec(ACTIVE_QUERY_NAME, ActivityAnalysis.class, ActivityAnalysis.class),
				null);
	}
	
	@PostConstruct
	public void init() {
		super.init(null, _container, _sourceQueue, _notificationTopic);

		_siCache = _container.getCache("serviceInvocations");
		_activitiesCache = _container.getCache("activities");
	}

	@PreDestroy
	public void close() {
		super.close();
	}

	@Override
	protected ActivityAnalysis processActivity(String sourceAQName, String id, ActiveChangeType changeType,
					int retriesLeft) throws Exception {
		ActivityAnalysis ret=null;
		
		Activity activity=_activitiesCache.get(id);
		
		if (activity == null) {
			throw new Exception("Failed to retrieve activity for query '"+
							getActiveQueryName()+"' and id '"+id+"'");
		}
		
		// Check if service interaction with correlation
		if (activity != null && activity.getServiceInvocation() != null &&
				activity.getServiceInvocation().getCorrelation() != null) {
			String correlation=activity.getServiceInvocation().getCorrelation();
			
			// Check if correlated invocation already exists
			ret = _siCache.get(correlation);
			
			if (ret == null) {
				// Create activity results object for correlated match
				ActivityAnalysis aa = new ActivityAnalysis();
				
				aa.addProperty("requestTimestamp", Long.class.getName(), activity.getTimestamp());
				aa.addProperty("requestId", String.class.getName(), id);
				aa.addProperty("serviceType", String.class.getName(), activity.getServiceInvocation().getServiceType());
				aa.addProperty("operation", String.class.getName(), activity.getServiceInvocation().getOperation());
				aa.addProperty("fault", String.class.getName(), activity.getServiceInvocation().getFault());
				aa.addProperty("principal", String.class.getName(), activity.getPrincipal());

				_siCache.put(correlation, aa, 150, TimeUnit.SECONDS);
			} else {
				long requestTimestamp=(Long)ret.getProperty("requestTimestamp").getValue();
				
				long responseTime=activity.getTimestamp()-requestTimestamp;
				
				ret.addProperty("responseTimestamp", Long.class.getName(), activity.getTimestamp());
				ret.addProperty("responseId", String.class.getName(), id);
				ret.addProperty("responseTime", Long.class.getName(), responseTime);
			}
		}
		
		return(ret);
	}
}
