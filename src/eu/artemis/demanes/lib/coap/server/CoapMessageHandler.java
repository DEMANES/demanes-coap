/**
 * File CoapMessageHandler.java
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

import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.ws4d.coap.interfaces.CoapMessage;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.messages.AbstractCoapMessage;
import org.ws4d.coap.messages.BasicCoapResponse;
import org.ws4d.coap.messages.CoapPacketType;
import org.ws4d.coap.messages.CoapResponseCode;

import eu.artemis.demanes.datatypes.ANES_URN;
import eu.artemis.demanes.lib.MessageDispatcher;
import eu.artemis.demanes.lib.SocketConnector;
import eu.artemis.demanes.lib.coap.CoapProxyConstants;
import eu.artemis.demanes.lib.impl.communication.CommUtils;
import eu.artemis.demanes.lib.services.RESTService;
import eu.artemis.demanes.lib.services.ServiceException;
import eu.artemis.demanes.lib.services.ServiceProvider;
import eu.artemis.demanes.lib.services.URNTranslator;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * CoapMessageHandler
 * 
 * Part of the server, this class is responsible for parsing the incoming
 * message and sending the requests to the registered service that deals with
 * this type of packet.
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 15 okt. 2014
 *
 */
public class CoapMessageHandler implements MessageDispatcher {

	private final Logger logger = Logger.getLogger("dmns:log");

	private final ServiceProvider serviceProvider;

	private final SocketConnector socketConnector;

	private final URNTranslator translator;

	// private ByteBuffer oldBuffer;

	public CoapMessageHandler(ServiceProvider sp, SocketConnector sc,
			URNTranslator ut) {
		this.serviceProvider = sp;
		this.socketConnector = sc;
		this.translator = ut;
		// oldBuffer = ByteBuffer.allocate(64);
		// oldBuffer.flip();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * At the top level, this is the server behavior. However, since it is
	 * possible that the incoming message is actually a response to a request,
	 * it is handled here first.
	 */
	@Override
	public ByteBuffer dispatchMessage(ByteBuffer buffer) {
		byte[] msg = new byte[buffer.remaining()];
		buffer.get(msg);

		// No use catching parsing exceptions, because then we cannot respond
		try {
			CoapMessage message = AbstractCoapMessage.parseMessage(msg,
					msg.length);

			if (message instanceof CoapRequest) {
				logger.trace(new LogEntry(this.getClass().getName(),
						LogConstants.LOG_LEVEL_TRACE, "Comm",
						"Received COAP Request: " + message + " -- ("
								+ CommUtils.asHex(message.getPayload()) + ")"));

				CoapMessage response = handleRequest((CoapRequest) message);

				// Return a response if the service gives one
				return (response == null ? null : ByteBuffer.wrap(response
						.serialize()));
			}
		} catch (Exception e) {
			logger.trace(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_TRACE, "Comm",
					"Received bytes cannot be cast as COAP message"));
		}

		return null;
	}

	/**
	 * Internal function used to make sure the input is a coapRequest, and the
	 * output is a CoapMessage.
	 * 
	 * @param message
	 * @return
	 */
	private CoapMessage handleRequest(CoapRequest msg) {
		byte[] payload = msg.getPayload();

		ANES_URN serviceID = null;

		// Get the service URN, and find the service that is registered to
		try {
			serviceID = getURNfromPayload(payload);
		} catch (URISyntaxException e) {
			logger.warn(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_WARN, "Comm",
					"Invalid URN received in COAPRequest " + serviceID));

			return CoapProxyConstants.createError(msg,
					CoapResponseCode.Bad_Request_400);
		}

		RESTService service = serviceProvider.resolve(serviceID);

		// RESTService not registered
		if (service == null) {
			logger.warn(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_WARN, "Comm",
					"Received COAP request for unknown service " + serviceID));

			return CoapProxyConstants.createError(msg,
					CoapResponseCode.Not_Found_404);
		}

		// Tell the source that we are busy coming up with an answer
		// No piggyback-ing always ACK immediately
		if (msg.getPacketType() == CoapPacketType.CON
				&& socketConnector != null) {
			CoapMessage ack = CoapProxyConstants.createAck(msg);
			if (ack != null)
				this.socketConnector.write(ack.serialize());
		}

		ByteBuffer serviceResponse;
		// Try to get a response from the service
		try {
			serviceResponse = passToService(service, msg);
		} catch (ServiceException e) {
			logger.error(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_ERROR, "Comm",
					"Error occured while handling Coap request", e));

			return CoapProxyConstants.createError(msg,
					CoapResponseCode.Bad_Option_402);
		}

		// If service gives no response, send nothing
		if (serviceResponse == null)
			return null;

		// Create the COAP response message (always NON because already ACKed)
		BasicCoapResponse response = new BasicCoapResponse(CoapPacketType.NON,
				CoapResponseCode.Content_205, msg.getMessageID(), null);

		response.setPayload(serviceResponse.array());

		return response;
	}

	/**
	 * @param service
	 * @param msg
	 * @return
	 * @throws ServiceException
	 */
	private ByteBuffer passToService(RESTService service, CoapRequest msg)
			throws ServiceException {
		// Pass only payload to the service message dispatcher
		ByteBuffer argument = getArgumentsfromPayload(msg.getPayload());

		switch (msg.getRequestCode()) {
		case GET:
			return service.get(argument);
		case PUT:
			return service.put(argument);
		case POST:
			return service.post(argument);
		case DELETE:
			return service.delete(argument);
		default:
			logger.error(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_ERROR, "Comm",
					"Invalid CoapRequestCode " + msg.getRequestCode()));

			throw new IllegalArgumentException("Invalid CoapRequestCode");
		}
	}

	/**
	 * Return the ANES_URN that should be at the start of the payload terminated
	 * by the service termination byte
	 * 
	 * @param payload
	 * @return
	 * @throws URISyntaxException
	 */
	public ANES_URN getURNfromPayload(byte[] payload) throws URISyntaxException {
		if (this.translator != null
				&& this.translator.byteToURN(payload[0]) != null)
			return this.translator.byteToURN(payload[0]);

		StringBuilder sb = new StringBuilder();
		for (byte b : payload)
			if (b == CoapProxyConstants.SERVICE_TERMINATOR)
				break;
			else
				sb.append((char) b);

		return new ANES_URN(sb.toString());
	}

	/**
	 * Return the arguments of the payload in a ByteBuffer or null if there are
	 * no arguments
	 * 
	 * @param payload
	 * @return
	 */
	public ByteBuffer getArgumentsfromPayload(byte[] payload) {
		int pos = 0;
		if (this.translator != null
				&& this.translator.byteToURN(payload[0]) != null) {
			pos = 1;
		} else {
			while (pos < payload.length)
				if (payload[pos++] == CoapProxyConstants.SERVICE_TERMINATOR)
					break;
		}

		if (pos == payload.length)
			return null;

		ByteBuffer ret = ByteBuffer.allocate(payload.length - pos);
		ret.put(payload, pos, ret.capacity()).flip();
		return ret;
	}

}
