/**
 * File CoapMessageClient.java
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
package eu.artemis.demanes.lib.coap.client;

import org.apache.log4j.Logger;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import eu.artemis.demanes.datatypes.ANES_URN;
import eu.artemis.demanes.lib.SocketConnector;
import eu.artemis.demanes.lib.services.RESTService;
import eu.artemis.demanes.lib.services.ServiceProvider;
import eu.artemis.demanes.lib.services.URNTranslator;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * CoapClient
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 15 okt. 2014
 *
 */
@Component(immediate = true, properties = "messageType=COAP")
public class CoapMessageClient implements ServiceProvider {

	private final Logger logger = Logger.getLogger("dmns:log");

	private SocketConnector socketConnector;

	private CoapRequestDispatcher requestDispatcher;

	private URNTranslator translator;

	@Activate
	public void start() {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "LifeCycle", "Activating module"));

		this.requestDispatcher = new CoapRequestDispatcher(socketConnector);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This is for the client side, say we want to USE the Coap as a Client and
	 * we want to use any particular service, we can use this interface to get
	 * that service.
	 */
	@Override
	public RESTService resolve(ANES_URN identifier) {
		logger.trace(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_TRACE, "Comm", "Finding REST Service "
						+ identifier));

		if (this.translator != null
				&& this.translator.URNToByte(identifier) != null)
			return new ServiceProxy(identifier, requestDispatcher,
					this.translator.URNToByte(identifier));
		else
			return new ServiceProxy(identifier, requestDispatcher);
	}

	@Reference
	public void setConnector(SocketConnector connector) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Reference",
				"Setting SocketConnector " + connector));

		this.socketConnector = connector;
	}

	@Reference(optional = true)
	public void setTranslator(URNTranslator ut) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Reference",
				"Setting URNTranslator " + ut));

		this.translator = ut;
	}
}
