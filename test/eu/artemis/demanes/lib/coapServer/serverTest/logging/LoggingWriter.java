/**
 * File LoggingWriter.java
 * 
 * This file is part of the eu.artemis.demanes.lib.coapParameterization project.
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
package eu.artemis.demanes.lib.coapServer.serverTest.logging;

import java.nio.ByteBuffer;

import org.ws4d.coap.interfaces.CoapMessage;
import org.ws4d.coap.messages.BasicCoapRequest;
import org.ws4d.coap.messages.CoapPacketType;
import org.ws4d.coap.messages.CoapRequestCode;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import eu.artemis.demanes.lib.MessageDispatcher;

/**
 * LoggingWriter
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 20 okt. 2014
 *
 */
@Component
public class LoggingWriter {

	private MessageDispatcher dispatcher;
	
	private Thread thread;

	@Reference(target = "(messageType=COAP)")
	public void setDispatcher(MessageDispatcher m) {
		System.out.println("Setting message dispatch of the tester!");
		this.dispatcher = m;
	}

	@Activate
	public void start() {
		this.thread = new Thread(new RandomRunner());
		this.thread.start();
	}

	@Deactivate
	public void stop() {
		this.thread.interrupt();
	}

	/**
	 * RandomRunner
	 *
	 * @author leeuwencjv
	 * @version 0.1
	 * @since 20 okt. 2014
	 *
	 */
	private class RandomRunner implements Runnable {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			int count = 0;
			try {
				while (true) {
					CoapMessage msg = new BasicCoapRequest(CoapPacketType.NON,
							CoapRequestCode.POST, count++);

					//String serviceprefix = "urn:dmns:log?";
					String logstr = "This is some type of a test for the logging service!!!";

					//ByteBuffer bb = ByteBuffer.allocate(serviceprefix.length()
					//		+ logstr.length() + 1);
					//bb.put(serviceprefix.getBytes());
					//bb.put((byte) logstr.length());
					ByteBuffer bb = ByteBuffer.allocate(logstr.length() + 1);
					bb.put((byte) 0xA1);
					bb.put(logstr.getBytes());
					msg.setPayload(bb.array());

					dispatcher
							.dispatchMessage(ByteBuffer.wrap(msg.serialize()));

					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}

	}

}
