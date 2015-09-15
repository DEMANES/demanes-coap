/**
 * File TestCoapServer2.java
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
package eu.artemis.demanes.lib.coapServer.junit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import org.junit.Test;

import eu.artemis.demanes.datatypes.ANES_URN;
import eu.artemis.demanes.lib.coap.server.CoapMessageHandler;

/**
 * TestCoapServer2
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 9 okt. 2014
 *
 */
public class TestCoapServer2 {

	private CoapMessageHandler cmh = new CoapMessageHandler(null, null, null);
	
	@Test
	public void testURNfromPayload() throws URISyntaxException {
		byte[] testbytes = "urn:dmsn:par?somethingcool".getBytes();
		ANES_URN urn = cmh.getURNfromPayload(testbytes);
		assertEquals("urn:dmsn:par", urn.toString());
	}

	@Test
	public void testArgumentfromPayload() throws URISyntaxException {
		byte[] testbytes = "urn:dmsn:par?somethingcool".getBytes();
		ByteBuffer arg = cmh.getArgumentsfromPayload(testbytes);
		assertArrayEquals("somethingcool".getBytes(), arg.array());
	}

	@Test(expected = URISyntaxException.class)
	public void testInvalidURN() throws URISyntaxException {
		byte[] testbytes = "gekkehenkies?somethingcool".getBytes();
		cmh.getURNfromPayload(testbytes);
		fail("An exception should have occured");
	}

	@Test
	public void testNoArgument() throws URISyntaxException {
		byte[] testbytes = "urn:dmsn:par".getBytes();
		ByteBuffer arg = cmh.getArgumentsfromPayload(testbytes);
		assertNull(arg);
		
		testbytes = "gekkehenkie".getBytes();
		arg = cmh.getArgumentsfromPayload(testbytes);
		assertNull(arg);
	}

}
