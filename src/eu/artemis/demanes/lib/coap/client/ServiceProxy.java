/**
 * File ServiceProxy.java
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

import org.ws4d.coap.messages.CoapRequestCode;

import eu.artemis.demanes.datatypes.ANES_URN;
import eu.artemis.demanes.lib.coap.CoapProxyConstants;
import eu.artemis.demanes.lib.services.RESTService;
import eu.artemis.demanes.lib.services.ServiceException;

/**
 * Internal class ServiceProxy
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 28 okt. 2014
 *
 */
final class ServiceProxy implements RESTService {

	private final ANES_URN identifier;

	private final CoapRequestDispatcher dispatcher;
	
	private final byte [] servicePrefix;

	public ServiceProxy(ANES_URN urn, CoapRequestDispatcher dispatcher) {
		ByteBuffer prefixBuf = ByteBuffer.allocate(urn.toString().length() + 1);
		prefixBuf.put(urn.toString().getBytes());
		prefixBuf.put(CoapProxyConstants.SERVICE_TERMINATOR);
		
		this.identifier = urn;
		this.dispatcher = dispatcher;
		this.servicePrefix = prefixBuf.array();
	}
	
	public ServiceProxy(ANES_URN urn, CoapRequestDispatcher dispatcher, byte serviceID) {
		this.identifier = urn;
		this.dispatcher = dispatcher;
		this.servicePrefix = new byte [] {serviceID};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer get(ByteBuffer input) throws ServiceException {
		return request(CoapRequestCode.GET, input);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer put(ByteBuffer input) throws ServiceException {
		return request(CoapRequestCode.PUT, input);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer post(ByteBuffer input) throws ServiceException {
		return request(CoapRequestCode.POST, input);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ByteBuffer delete(ByteBuffer input) throws ServiceException {
		return request(CoapRequestCode.DELETE, input);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ANES_URN identifier() {
		return identifier;
	}

	private ByteBuffer request(CoapRequestCode rc, ByteBuffer args)
			throws ServiceException {
		ByteBuffer payload;
		if (args == null) {
			// Only request the service without arguments
			payload = ByteBuffer.wrap(this.servicePrefix);
		} else {
			// Create the message payload
			payload = ByteBuffer.allocate(args.remaining()
					+ this.servicePrefix.length);

			payload.put(this.servicePrefix);
			payload.put(args.slice());
		}

		// Use the dispatchRequest from the MessageClient
		byte[] response = dispatcher.dispatchRequest(rc, payload.array());

		if (response == null)
			return null;
		else
			return ByteBuffer.wrap(response);
	}
}
