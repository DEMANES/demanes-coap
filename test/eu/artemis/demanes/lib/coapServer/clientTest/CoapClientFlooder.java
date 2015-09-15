/**
 * File CoapClientFlooder.java
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
package eu.artemis.demanes.lib.coapServer.clientTest;

import java.nio.ByteBuffer;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import eu.artemis.demanes.datatypes.ANES_URN;
import eu.artemis.demanes.lib.services.RESTService;
import eu.artemis.demanes.lib.services.ServiceException;
import eu.artemis.demanes.lib.services.ServiceProvider;

/**
 * CoapClientFlooder
 *
 * @author leeuwencjv
 * @version 0.1
 * @since 17 okt. 2014
 *
 */
@Component
public class CoapClientFlooder {

	private ServiceProvider serviceprovider;

	private Thread internalThread;

	@Activate
	public void start() {
		internalThread = new Thread(new ServiceRunner());
		internalThread.start();
	}

	@Deactivate
	public void stop() {
		internalThread.interrupt();
	}

	@Reference(target = "(messageType=COAP)")
	public void setServiceProvider(ServiceProvider sp) {
		this.serviceprovider = sp;
	}

	private class ServiceRunner implements Runnable {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			RESTService p = serviceprovider
					.resolve(new ANES_URN("dmns", "par"));
			ByteBuffer payload = ByteBuffer.allocate(255);

			String parameterName = "samplingfrequency";
			int newValue = 4;

			try {
				while (true) {
					payload.clear();
					payload.put((byte) parameterName.length());
					payload.put(parameterName.getBytes());
					payload.flip();

					p.get(payload);
					
					Thread.sleep(1000);

					payload.clear();
					payload.put((byte) parameterName.length());
					payload.put(parameterName.getBytes());
					payload.put((byte) (Integer.SIZE / 8));
					payload.putInt(newValue);
					payload.flip();

					p.post(payload);
					
					Thread.sleep(1000);
				}

			} catch (InterruptedException e) {
				// Do nothing
			} catch (ServiceException e) {
				// Do nothing
			}

		}

	}

}
