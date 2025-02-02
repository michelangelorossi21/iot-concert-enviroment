package it.unimore.dipi.iot.client.Put;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A simple CoAP Synchronous Client implemented using Californium Java Library
 * The simple client send a PUT request to a target CoAP Resource with some custom request parameters
 * and payload
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smartobject
 * @created 20/10/2020 - 09:19
 */
public class CoapPutClientProcessLight_Brightness {

	private final static Logger logger = LoggerFactory.getLogger(CoapPutClientProcessLight_Brightness.class);

	//private static final String COAP_ENDPOINT = "coap://127.0.0.1:5684/light/brightness";

	public static void main(String[] args) {

		//Initialize coapClient
		String coap_endpoint = args[0];
		CoapClient coapClient = new CoapClient(coap_endpoint);

		//Request Class is a generic CoAP message: in this case we want a PUT.
		//"Message ID", "Token" and other header's fields can be set
		Request request = new Request(Code.PUT);

		//Set PUT request's payload
		String myPayload = args[1];
		request.setPayload(myPayload);

		//Set Request as Confirmable
		request.setConfirmable(true);

		//Synchronously send the POST request (blocking call)
		CoapResponse coapResp = null;

		try {

			coapClient.setTimeout(1000L);

			coapResp = coapClient.advanced(request);

			for (String s:args) {
				logger.info(s);
			}

		} catch (ConnectorException | IOException e) {
			e.printStackTrace();
		}
	}
}