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
package org.savara.sam.demo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.savara.sam.activity.ActivityModel.*;
import org.savara.sam.activity.ServiceModel.*;
import org.savara.sam.collector.ActivityCollector;
import org.savara.sam.internal.collector.JMSActivityCollectorImpl;

public class EventGenerator {
	
	private static String[] _principals=new String[] { "gary", "jeff", "steve", "viv", "joe", "jane", "john", "lisa" };
	private static String[] _scenarioNames=new String[] { "SuccessfulPurchase",
									"InvalidStoreBehaviour", "CustomerUnknown",
									//"InvalidStoreLogic",  - not included for now, as assertions are not supported in the monitor yet
									"InsufficientCredit" };
	private static int[] _scenarioWeightings=new int[] { 12, 1, 2, 
									//1, - associated with InvalidStoreLogic
									4 };
	
	private org.savara.scenario.model.Scenario[] _scenarios=
					new org.savara.scenario.model.Scenario[_scenarioNames.length];
	private java.util.Properties _roleToServiceType=new java.util.Properties();
	private java.util.Properties _clientServerRoles=new java.util.Properties();
	
	private ActivityCollector _collector=null;
	
	private java.util.Random _random=new java.util.Random(System.currentTimeMillis());
	
	private java.util.concurrent.ThreadPoolExecutor _executor=
			new java.util.concurrent.ThreadPoolExecutor(200, 200, 10,
					TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10000));

	public static void main(String[] args) throws Exception {
		EventGenerator egen=new EventGenerator();
		
		egen.init();
		
		egen.run();
		
		System.out.println("Exiting event generator.");
	}
	
	public void init() throws Exception {
		
		// Initialize scenarios
		for (int i=0; i < _scenarioNames.length; i++) {
			String path="/scenarios/"+_scenarioNames[i]+".scn";
			
			java.io.InputStream is=EventGenerator.class.getResourceAsStream(path);
			
			_scenarios[i] = org.savara.scenario.util.ScenarioModelUtil.deserialize(is);
		}
		
		java.io.InputStream is=EventGenerator.class.getResourceAsStream("/scenarios/RoleServiceTypes.properties");
		_roleToServiceType.load(is);
		
		is = EventGenerator.class.getResourceAsStream("/scenarios/ClientServerRoles.properties");
		_clientServerRoles.load(is);
	}
	
	public void run() {
		
		_collector = new JMSActivityCollectorImpl();
		((JMSActivityCollectorImpl)_collector).init();
		
		while (displayOptions() != 0);
		
		((JMSActivityCollectorImpl)_collector).close();
		
		_executor.shutdown();
	}
	
	protected int getInt(int def) {
		int ret=0;
		
		try {
			byte[] b=new byte[1024];
			System.in.read(b);
			
			String str=new String(b).trim();
			
			if (str.trim().length() == 0) {
				ret = def;
			} else {
				ret = Integer.parseInt(str);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return(ret);
	}
	
	protected int displayOptions() {
		
		System.out.println("\r\n-----------------------------\r\n");
		System.out.println("0) Exit");
		
		for (int i=0; i < _scenarioNames.length; i++) {
			System.out.println((i+1)+") Enact scenario '"+_scenarioNames[i]+"'");
		}
		
		System.out.println((_scenarioNames.length+1)+") Performance test");
		System.out.println((_scenarioNames.length+2)+") Random scenario");
		
		System.out.println("Enter option:");
		
		final int ret = getInt(0);
		
		if (ret != 0) {
			System.out.println("Enter number of iterations (default 1):");
			int repeat=getInt(1);
			
			for (int i=0; i < repeat; i++) {
				
				if (ret < (_scenarioNames.length+1)) {
					
					_executor.execute(new Runnable() {
						public void run() {						
							runScenario(ret-1, "id"+_random.nextLong(), getPrincipal(), false);
						}
					});
				} else if (ret == (_scenarioNames.length+1)) {
					
					_executor.execute(new Runnable() {
						public void run() {						
							runScenario(1, "id"+_random.nextLong(), getPrincipal(), true);
						}
					});

				} else {
					_executor.execute(new Runnable() {
						public void run() {
							
							// Select scenario to run
							int max=0;
							
							for (int weight : _scenarioWeightings) {
								max += weight;
							}
							
							int weighted=(int)Math.round(Math.random()*max);
							
							int scenario=0;
							int cur=0;
							
							for (scenario=0; scenario < _scenarioWeightings.length; scenario++) {
								cur += _scenarioWeightings[scenario];
								
								if (cur >= weighted) {
									break;
								}
							}
							
							String id="id"+_random.nextLong();
							
							System.out.println("Running scenario '"+_scenarioNames[scenario]+"' id="+id);

							runScenario(scenario, id, getPrincipal(), false);
						}
					});
				}
			}
		}
		
		return(ret);
	}
	
	protected void runScenario(int scenarioNum, String id, String principal, boolean ignoreDelay) {
		org.savara.scenario.model.Scenario scenario=_scenarios[scenarioNum];
		
		java.util.List<String> correlations=new java.util.ArrayList<String>();
		
		for (org.savara.scenario.model.Event event : scenario.getEvent()) {
			
			if (event instanceof org.savara.scenario.model.MessageEvent) {
				org.savara.scenario.model.MessageEvent me=(org.savara.scenario.model.MessageEvent)event;
				
				String roleName=((org.savara.scenario.model.Role)me.getRole()).getName();
				
				ComponentId cid=ComponentId.newBuilder().setComponentType(roleName).
									setInstanceId(roleName+"-"+id).build();

				ServiceInvocation.Builder siBuilder=ServiceInvocation.newBuilder();
				
				String correlation=me.getOperationName()+"-"+roleName+"-"+id;
				boolean request=!correlations.contains(correlation);
				ServiceInvocation.Direction direction=
							me instanceof org.savara.scenario.model.ReceiveEvent ?
									ServiceInvocation.Direction.INBOUND :
									ServiceInvocation.Direction.OUTBOUND;
				
				String serverRole=roleName;
				
				if ((request && direction == ServiceInvocation.Direction.OUTBOUND) ||
						(!request && direction == ServiceInvocation.Direction.INBOUND)) {
					serverRole = _clientServerRoles.getProperty(roleName+"."+me.getOperationName());
				}
				
				correlations.add(correlation);
				
				siBuilder.setServiceType(_roleToServiceType.getProperty(serverRole)).
							setCorrelation(correlation).
							setOperation(me.getOperationName()).
							setInvocationType(request ? ServiceInvocation.InvocationType.REQUEST :
								ServiceInvocation.InvocationType.RESPONSE).
							setDirection(direction);
				
				if (me.getFaultName() != null && me.getFaultName().trim().length() > 0) {
					siBuilder.setFault(me.getFaultName());
				}
				
				for (org.savara.scenario.model.Parameter p : me.getParameter()) {
					Message m=Message.newBuilder().setMessageType(p.getType()).
							setContent(getMessageContent(id, p.getValue())).build();
					
					siBuilder.addMessage(m);
				}

				Activity activity=Activity.newBuilder().setId(cid).setTimestamp(System.currentTimeMillis()).
							setServiceInvocation(siBuilder.build()).setPrincipal(principal).build();

				_collector.process(activity);
				
			} else if (!ignoreDelay && event instanceof org.savara.scenario.model.TimeElapsedEvent) {
				org.savara.scenario.model.TimeElapsedEvent tee=
						(org.savara.scenario.model.TimeElapsedEvent)event;
				
				try {
					long duration=Long.parseLong(tee.getDuration());
					
					long actualDelay=getDelay(duration);
					
					Thread.sleep(actualDelay);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	protected String getMessageContent(String id, String name) {
		String ret=null;
		String path="messages/"+name;
		
		if (!path.endsWith(".xml")) {
			path += ".xml";
		}
		
		java.io.InputStream is=ClassLoader.getSystemResourceAsStream(path);
		
		if (is == null) {
			is = EventGenerator.class.getResourceAsStream("/"+path);
		}
		
		if (is != null) {
			try {
				byte[] b=new byte[is.available()];
				is.read(b);
				is.close();
				
				ret = new String(b);
				
				ret = ret.replaceAll("%CID%", id);
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Failed to get message content for '"+name+"'");
		}
		
		return(ret);
	}

	protected String getPrincipal() {
		int pos=(int)(Math.random() * _principals.length);
		return(_principals[pos]);
	}
	
	protected long getDelay(long duration) {
		int val=(int)(Math.random() * duration);
		return(duration + val);
	}
}
