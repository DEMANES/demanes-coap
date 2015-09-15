/**
 * File CoapRequestDispatcher.java
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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.ws4d.coap.interfaces.CoapMessage;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.AbstractCoapMessage;
import org.ws4d.coap.messages.BasicCoapRequest;
import org.ws4d.coap.messages.CoapPacketType;
import org.ws4d.coap.messages.CoapRequestCode;
import org.ws4d.coap.messages.CoapResponseCode;

import eu.artemis.demanes.lib.MessageDispatcher;
import eu.artemis.demanes.lib.MessageDispatcherRegistry;
import eu.artemis.demanes.lib.SocketConnector;
import eu.artemis.demanes.lib.coap.CoapProxyConstants;
import eu.artemis.demanes.lib.impl.communication.CommUtils;
import eu.artemis.demanes.lib.services.ServiceException;
import eu.artemis.demanes.logging.LogConstants;
import eu.artemis.demanes.logging.LogEntry;

/**
 * CoapRequestDispatcher
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 28 okt. 2014
 *
 */
public class CoapRequestDispatcher implements MessageDispatcher {

	private final Logger logger = Logger.getLogger("dmns:log");

	// Default timeout in milliseconds
	private static final long DEFAULT_REQUEST_TIMEOUT = 1000;

	private static int maxMessageID = 1;

	private final Map<Integer, SynchronousQueue<CoapResponse>> runningRequests;

	private final SocketConnector connector;

	/**
	 * @param socketConnector
	 */
	public CoapRequestDispatcher(SocketConnector socketConnector) {
		this.runningRequests = new HashMap<Integer, SynchronousQueue<CoapResponse>>();
		this.connector = socketConnector;
	}

	/**
	 * @param message
	 * @return
	 */
	public CoapMessage handleResponse(CoapResponse message) {
		Integer msgID = message.getMessageID();
		SynchronousQueue<CoapResponse> queue = this.runningRequests.get(msgID);

		// If found, try to put the payload in the queue for who is waiting
		boolean success = (queue != null) && queue.offer(message);

		if (success & message.getPacketType() == CoapPacketType.CON) {
			return CoapProxyConstants.createAck(message); // This should be it
															// right?
			// return new CoapEmptyMessage(CoapPacketType.ACK, msgID);
		} else if (!success) {
			logger.warn(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_WARN, "Comm",
					"Cannot handle CoapResponse: No matching request found"));

			return CoapProxyConstants.createError(message,
					CoapResponseCode.Not_Found_404);
		} else
			return null;
	}

	/**
	 * Dispatch a COAP request message. If a response is returned to this
	 * particular request within the default timeout periods (
	 * {@value #DEFAULT_REQUEST_TIMEOUT} milliseconds), the function will return
	 * the payload of the response message. If there is no response, or it is
	 * too late, the function will return null.
	 * 
	 * @param requestCode
	 *            The request code of the COAP request
	 * @param payload
	 *            The query to send
	 * @return
	 */
	public byte[] dispatchRequest(CoapRequestCode requestCode, byte[] payload)
			throws ServiceException {
		try {
			CoapMessage req = this.sendRequest(requestCode, payload);

			SynchronousQueue<CoapResponse> responseQueue = new SynchronousQueue<CoapResponse>();
			this.runningRequests.put(req.getMessageID(), responseQueue);

			// And then we should wait...
			CoapResponse response = responseQueue.poll(DEFAULT_REQUEST_TIMEOUT,
					TimeUnit.MILLISECONDS);

			// Always remove?
			this.runningRequests.remove(req.getMessageID());

			if (response == null)
				return null;
			else
				switch (response.getResponseCode()) {
				case Created_201:
				case Deleted_202:
				case Valid_203:
				case Changed_204:
				case Content_205:
					return response.getPayload();
				default:
					logger.error(new LogEntry(this.getClass().getName(),
							LogConstants.LOG_LEVEL_ERROR, "Comm",
							"Error in using coapService. ("
									+ response.getResponseCode() + ")"));

					throw new ServiceException("Error in using coapService. ("
							+ response.getResponseCode() + ")");
				}

		} catch (InterruptedException e) {
			logger.fatal(new LogEntry(this.getClass().getName(),
					LogConstants.LOG_LEVEL_ERROR, "Comm",
					"RequestDispatcher interrupted.", e));

			throw new ServiceException(e);
		}
	}

	/**
	 * Send a COAP request message. No return message is handled by the
	 * MessageServer, if a response comes back and specifically targets the
	 * service, the message will be relayed. If no service is mentioned in the
	 * response, the response to the request will end up as a 404.
	 * 
	 * @param requestCode
	 * @param payload
	 * @return
	 */
	private CoapMessage sendRequest(CoapRequestCode requestCode, byte[] payload) {
		CoapMessage req = new BasicCoapRequest(CoapPacketType.NON, requestCode,
				maxMessageID++);

		// Just so that I skip the evil number 10.
		if (maxMessageID == 10)
			maxMessageID++;

		req.setPayload(payload);

		MessageDispatcherRegistry server = connector.write(req.serialize());

		if (!server.containsDispatcher(this))
			server.addDispatcher(this);

		return req;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.artemis.demanes.lib.MessageDispatcher#dispatchMessage(java.nio.ByteBuffer
	 * )
	 */
	@Override
	public ByteBuffer dispatchMessage(ByteBuffer buffer) {
		byte[] msg = new byte[buffer.remaining()];
		buffer.get(msg);

		// No use catching parsing exceptions, because then we cannot respond
		try {
			CoapMessage message = AbstractCoapMessage.parseMessage(msg,
					msg.length);

			if (message instanceof CoapResponse) {
				logger.trace(new LogEntry(this.getClass().getName(),
						LogConstants.LOG_LEVEL_TRACE, "Comm",
						"Received COAP Response: " + message + " -- ("
								+ CommUtils.asHex(message.getPayload()) + ")"));

				CoapMessage response = this
						.handleResponse((CoapResponse) message);

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

}
