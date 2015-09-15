/**
 * File CoapProxyConstants.java
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
package eu.artemis.demanes.lib.coap;

import org.ws4d.coap.interfaces.CoapMessage;
import org.ws4d.coap.messages.BasicCoapResponse;
import org.ws4d.coap.messages.CoapEmptyMessage;
import org.ws4d.coap.messages.CoapPacketType;
import org.ws4d.coap.messages.CoapResponseCode;

/**
 * CoapProxyConstants
 * 
 * Defines the constants used in the COAP protocol defined by DEMANES <b>ON TOP
 * OF</b> the regular COAP definition. For more information on the used
 * protocols see the wiki.
 *
 * @see <a
 *      href=http://wiki.demanes.eu/index.php/COAP_for_Demanes>http://wiki.demanes
 *      .eu/index.php/COAP_for_Demanes</a>
 * @author leeuwencjv
 * @version 0.1
 * @since 15 okt. 2014
 *
 */
public final class CoapProxyConstants {

	/* Character to terminate label */
	public static final byte SERVICE_TERMINATOR = '?';
	
	/* Boolean whether we send acks */
	private static boolean SEND_ACKS = false;

	/* Boolean whether we send errors */
	private static boolean SEND_ERRORS = false;
	
	/**
	 * Package private function for create Error messages if required.
	 * 
	 * @param message
	 * @param responseCode
	 * @return
	 */
	public static CoapMessage createError(CoapMessage message,
			CoapResponseCode responseCode) {
		if (CoapProxyConstants.SEND_ERRORS) {
			return new BasicCoapResponse(CoapPacketType.NON, responseCode,
					message.getMessageID(), null);
		} else {
			return null;
		}
	}

	/**
	 * Package private function for create Acknowledgement messages if required.
	 * 
	 * @param response
	 * @return
	 */
	public static CoapMessage createAck(CoapMessage msg) {
		if (CoapProxyConstants.SEND_ACKS
				&& msg.getPacketType() == CoapPacketType.CON) {
			return new CoapEmptyMessage(CoapPacketType.ACK, msg.getMessageID());
		} else {
			return null;
		}
	}
	
}
