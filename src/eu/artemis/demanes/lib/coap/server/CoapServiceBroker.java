/**
 * File CoapServiceBroker.java
 * 
 * This file is part of the eu.artemis.demanes.lib.coapServer project.
 *
 * Copyright 2014 TNO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.artemis.demanes.lib.coap.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.artemis.demanes.datatypes.ANES_URN;
import eu.artemis.demanes.lib.services.ServiceProvider;
import eu.artemis.demanes.lib.services.ServiceRegistry;
import eu.artemis.demanes.lib.services.RESTService;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * CoapServiceBroker
 * 
 * This contains the collection of COAP services that are available in the
 * system. It keeps a Map of the service identifiers to the services. Services
 * can register at it, and it will make it available to other modules using the
 * ServiceProvider interface.
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 15 okt. 2014
 *
 */
public class CoapServiceBroker implements ServiceRegistry, ServiceProvider {

	private final Logger logger = Logger.getLogger("dmns:log");
	
	private Map<ANES_URN, RESTService> serviceMap;

	public CoapServiceBroker() {
		this.serviceMap = new HashMap<ANES_URN, RESTService>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerService(RESTService s) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Comm",
				"Registering coap service with urn " + s.identifier()));

		this.serviceMap.put(s.identifier(), s);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unregisterService(RESTService s) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Comm",
				"Unregistering coap service with urn " + s.identifier()));
		
		this.serviceMap.remove(s.identifier());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RESTService resolve(ANES_URN identifier) {
		return this.serviceMap.get(identifier);
	}

}
