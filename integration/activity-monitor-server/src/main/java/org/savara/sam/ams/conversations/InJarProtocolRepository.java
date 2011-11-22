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
package org.savara.sam.ams.conversations;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.savara.protocol.ProtocolCriteria;
import org.savara.protocol.ProtocolId;
import org.savara.protocol.repository.ProtocolRepository;
import org.scribble.common.resource.ResourceContent;
import org.scribble.protocol.DefaultProtocolContext;
import org.scribble.protocol.ProtocolContext;
import org.scribble.protocol.model.ProtocolModel;
import org.scribble.protocol.model.Role;

/**
 * Protocol repository implementation that loads models from
 * the classpath.
 *
 */
public class InJarProtocolRepository implements ProtocolRepository {

	private static final Logger LOG=Logger.getLogger(InJarProtocolRepository.class.getName());
	
	private ProtocolModel _model=null;
	private java.util.Map<ProtocolId,ProtocolModel> _localModels=
					new java.util.HashMap<ProtocolId,ProtocolModel>();
	
	public InJarProtocolRepository(java.net.URI uri, String... roles) {
		
		org.scribble.protocol.parser.ProtocolParser parser=
				new org.savara.pi4soa.cdm.parser.CDMProtocolParser();
		org.scribble.protocol.projection.ProtocolProjector projector=
				new org.scribble.protocol.projection.impl.ProtocolProjectorImpl();
		ProtocolContext context=new DefaultProtocolContext();
		
		org.scribble.common.logging.Journal journal=
				new org.scribble.common.logging.CachedJournal();
		
		try {
			_model = parser.parse(context, new ResourceContent(uri), journal);

			// If roles not explicitly defined, then initialise all roles
			if (roles == null || roles.length == 0) {
				java.util.List<Role> rlist=_model.getRoles();
				roles = new String[rlist.size()];
				for (int i=0; i < rlist.size(); i++) {
					roles[i] = rlist.get(i).getName();
				}
			}
			
			// Project to relevant roles
			for (String role : roles) {
				ProtocolId pid=new ProtocolId(_model.getProtocol().getName(), role);
				
				ProtocolModel lm=projector.project(context, _model,
								new Role(role), journal);
				
				_localModels.put(pid, lm);
			}
			
		} catch(Exception e) {
			LOG.log(Level.SEVERE, "Failed to parse protocol model '"+uri+"'", e);
		}
	}

	public ProtocolModel getProtocol(ProtocolId pid) {
		return (_localModels.get(pid));
	}

	public List<ProtocolId> getProtocols(ProtocolCriteria criteria) {
		// TODO: Need to filter out the protocols that are related to
		// the supplied criteria (e.g. message types etc).
		List<ProtocolId> ret=new java.util.Vector<ProtocolId>(_localModels.keySet());
		
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("Get protocol ids for criteria="+criteria+" ret="+ret);
		}
		
		return (ret);
	}
}
