package it.unimore.dipi.iot.client.Post;

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
public class CoapPostClientProcess_switch {

	private final static Logger logger = LoggerFactory.getLogger(CoapPostClientProcess_switch.class);

	private static final String COAP_ENDPOINT = "coap://127.0.0.1:5684/main_light/switch";

	public static void main(String[] args) {

		//Initialize coapClient
		CoapClient coapClient = new CoapClient(COAP_ENDPOINT);

		//Request Class is a generic CoAP message: in this case we want a PUT.
		//"Message ID", "Token" and other header's fields can be set
		Request request = new Request(Code.POST);

		//Set Request as Confirmable
		request.setConfirmable(true);

		//Synchronously send the POST request (blocking call)
		CoapResponse coapResp = null;

		try {

			coapClient.setTimeout(1000L);

			coapResp = coapClient.advanced(request);


		} catch (ConnectorException | IOException e) {
			e.printStackTrace();
		}
	}
}