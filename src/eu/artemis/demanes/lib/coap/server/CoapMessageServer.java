/**
 * File CoapMessageServer.java
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

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import eu.artemis.demanes.lib.MessageDispatcher;
import eu.artemis.demanes.lib.SocketConnector;
import eu.artemis.demanes.lib.services.RESTService;
import eu.artemis.demanes.lib.services.ServiceRegistry;
import eu.artemis.demanes.lib.services.URNTranslator;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * COAPMessageDispatcher
 * 
 * @author leeuwencjv
 * @version 0.1
 * @since 9 okt. 2014
 * 
 */
@Component(immediate = true, properties = "messageType=COAP")
public class CoapMessageServer implements ServiceRegistry, MessageDispatcher {

	private final Logger logger = Logger.getLogger("dmns:log");

	private final CoapServiceBroker broker = new CoapServiceBroker();

	private SocketConnector connector;

	private MessageDispatcher messageHandler;

	private URNTranslator translator;

	@Activate
	public void start() {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "LifeCycle", "Activating module"));

		this.messageHandler = new CoapMessageHandler(this.broker,
				this.connector, this.translator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.artemis.demanes.lib.MessageDispatcher#dispatchMessage(java.nio.ByteBuffer
	 * )
	 */
	@Override
	public ByteBuffer dispatchMessage(ByteBuffer msg) {
		return messageHandler.dispatchMessage(msg);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This is for the server side, say we have a service and want to provide
	 * this service via Coap, then use this interface to register/unregister
	 * those services.
	 */
	@Override
	@Reference(optional = true, dynamic = true, multiple = true)
	public void registerService(RESTService s) {
		this.broker.registerService(s);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This is for the server side, say we have a service and want to provide
	 * this service via Coap, then use this interface to register/unregister
	 * those services.
	 */
	@Override
	public void unregisterService(RESTService s) {
		this.broker.unregisterService(s);
	}

	/**
	 * Setting the SocketConnector is optional. If there is no socket connector
	 * defined, the only result is that immediate acknowledgements won't be
	 * sent. If there is one, then it may be possible.
	 * 
	 * @param s
	 */
	@Reference(optional = true, multiple = false, dynamic = false)
	public void setSocketConnector(SocketConnector s) {
		this.connector = s;
	}

	/**
	 * Provide a URN translator for creating messages compatible with the
	 * reduced COAP format
	 * 
	 * @param ut
	 */
	@Reference(optional = true)
	public void setTranslator(URNTranslator ut) {
		logger.debug(new LogEntry(this.getClass().getName(),
				LogConstants.LOG_LEVEL_DEBUG, "Reference",
				"Setting URNTranslator " + ut));

		this.translator = ut;
	}
}
