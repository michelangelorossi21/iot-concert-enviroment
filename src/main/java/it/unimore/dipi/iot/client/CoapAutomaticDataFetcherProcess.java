package it.unimore.dipi.iot.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.client.Put.CoapPutClientProcessInstrument_Volume;
import it.unimore.dipi.iot.client.Put.CoapPutClientProcessLight_Brightness;
import it.unimore.dipi.iot.client.Put.CoapPutClientProcess_switch;
import it.unimore.dipi.iot.utils.CoreInterfaces;
import it.unimore.dipi.iot.utils.SenMLPack;
import org.eclipse.californium.core.*;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * A simple CoAP automatic data fetcher.
 * Discover available resource on a target CoAP Smart Object and then start observing core.a and core.s available and
 * observable resources.
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smartobject
 * @created 20/10/2020 - 09:19
 */
public class CoapAutomaticDataFetcherProcess {

	private final static Logger logger = LoggerFactory.getLogger(CoapAutomaticDataFetcherProcess.class);

	private static final String TARGET_SMART_OBJECT_ADDRESS = "127.0.0.1";

	private static final int TARGET_SMART_OBJECT_IP = 5684;

	private static final String WELL_KNOWN_CORE_URI = "/.well-known/core";

	private static final String OBSERVABLE_CORE_ATTRIBUTE = "obs";

	private static final String INTERFACE_CORE_ATTRIBUTE = "if";

	private static final String CONTENT_TYPE_ATTRIBUTE = "ct";

	private static List<TargetCoapResourceDescriptor> targetObservableResourceList = null;

	private static Map<String, CoapObserveRelation> observingRelationMap = null;

	private static ObjectMapper objectMapper;
	public static ArrayList<Double> total_energy_consumption = new ArrayList<>();
	public static ArrayList<Double> temperature_mean = new ArrayList<>();
	public static ArrayList<Double> decibel_mean = new ArrayList<>();

	public static void main(String[] args) {

		//Init target resource list array and observing relations
		targetObservableResourceList = new ArrayList<>();
		observingRelationMap = new HashMap<>();
		objectMapper = new ObjectMapper();

		//Initialize coapClient
		CoapClient coapClient = new CoapClient();

		//Discover available observable core.a and core.s resources on the target node
		discoverTargetObservableResources(coapClient);

		//Start observing each resource
		targetObservableResourceList.forEach(targetResource -> {
			startObservingTargetResource(coapClient, targetResource.getTargetUrl(), targetResource.getSenmlSupport(), total_energy_consumption, temperature_mean, decibel_mean);
			//logger.warn(targetResource.getTargetUrl());
		});

		List<String> url = targetObservableResourceList.stream().map(targetResource -> targetResource.getTargetUrl()).collect(Collectors.toList());


		//Sleep and then cancel registrations
		try {
			Thread.sleep(3000*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		observingRelationMap.forEach((key, value) -> {
			logger.info("Canceling Observation for target Url: {}", key);
			value.proactiveCancel();
		});


	}

	private static void startObservingTargetResource(CoapClient coapClient, String targetUrl, boolean useSenml,
													 ArrayList<Double> total_energy_consumption,
													 ArrayList<Double> temperature_mean,
													 ArrayList<Double> decibel_mean) {

		logger.info("OBSERVING ... {}", targetUrl);

		Request request = Request.newGet();

		if(useSenml)
			request.setOptions(new OptionSet().setAccept(MediaTypeRegistry.APPLICATION_SENML_JSON));

		request.setObserve();
		request.setConfirmable(true);
		request.setURI(targetUrl);

		CoapObserveRelation relation = coapClient.observe(request, new CoapHandler() {

			public void onLoad(CoapResponse response) {
				String content = response.getResponseText();
				//logger.warn(content);

				// DATA ANALYSIS
				// 1. Check lights temperature:
				if(String.valueOf(targetUrl).endsWith("temperature")) {
					try {
						SenMLPack senMLPack = objectMapper.readValue(content, SenMLPack.class);
						double v = (double) senMLPack.get(0).getV();
						logger.info("NEW TEMPERATURE MEASUREMENT: {} FROM {}", String.valueOf(v), targetUrl);
						if (v >= 75) {
							logger.error("WARNING!!! HIGH TEMPERATURE " + String.valueOf(v));
							logger.warn("DECREASING BRIGHTNESS");
							CoapPutClientProcessLight_Brightness.main(new String[] {targetUrl, "10"});
						}
						// 1b. Compute temperature mean:
						temperature_mean.add(v);


					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}

				// 2. Check acoustic volume:
				if(String.valueOf(targetUrl).endsWith("acoustic")) {
					try {
						SenMLPack senMLPack = objectMapper.readValue(content, SenMLPack.class);
						double v = (double) senMLPack.get(0).getV();
						logger.info("NEW DECIBEL MEASUREMENT: {} FROM {}", String.valueOf(v), targetUrl);
						if (v >= 115){
							logger.error("WARNING!!! HIGH DECIBEL MEASUREMENT: " + String.valueOf(v));
							logger.warn("DECREASING VOLUME");
							CoapPutClientProcessInstrument_Volume.main(new String[]{targetUrl, "10"});
						}
						// 2b. Compute decibel mean:
						decibel_mean.add(v);

					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}

				// 3. Check single energy consumptions:
				if(String.valueOf(targetUrl).endsWith("energy")) {
					try {
						SenMLPack senMLPack = objectMapper.readValue(content, SenMLPack.class);
						double v = (double) senMLPack.get(0).getV();
						logger.info("NEW ENERGY VALUE RETRIEVED: {} FROM {}", String.valueOf(v), targetUrl);

						// Compute total Energy consumption:
						total_energy_consumption.add(v);

					} catch (IOException e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}

				// 4. Temperature mean, decibel mean, Total energy consumption:
				double temp_mean = compute_sum(temperature_mean)/temperature_mean.size();
				logger.info("TEMPERATURE MEAN: " + String.valueOf(temp_mean));

				double db_mean = compute_sum(decibel_mean)/decibel_mean.size();
				logger.info("DECIBEL MEAN: " + String.valueOf(db_mean));

				double sum = compute_sum(total_energy_consumption);
				logger.info("TOTAL ENERGY CONSUMPTION: " + String.valueOf(sum));

			}

			public double compute_sum (ArrayList<Double> list) {
				double sum = 0.0;
				for (int i=0; i<list.size(); i++){
					sum += list.get(i);
				}
				return sum;
			}

			public void onError() {
				logger.error("OBSERVING {} FAILED", targetUrl);
			}
		});

		observingRelationMap.put(targetUrl, relation);

	}


	private static void discoverTargetObservableResources(CoapClient coapClient){

		//Request Class is a generic CoAP message: in this case we want a GET.
		//"Message ID", "Token" and other header's fields can be set
		Request request = new Request(Code.GET);

		//coap://127.0.0.1:5683/.well-known/core
		request.setURI(String.format("coap://%s:%d%s",
				TARGET_SMART_OBJECT_ADDRESS,
				TARGET_SMART_OBJECT_IP,
				WELL_KNOWN_CORE_URI));

		//Set Request as Confirmable
		request.setConfirmable(true);

		//logger.info("Request Pretty Print: \n{}", Utils.prettyPrint(request));

		//Synchronously send the GET message (blocking call)
		CoapResponse coapResp = null;

		try {

			coapResp = coapClient.advanced(request);

			if(coapResp != null) {

				//Pretty print for the received response
				//logger.info("Response Pretty Print: \n{}", Utils.prettyPrint(coapResp));

				if (coapResp.getOptions().getContentFormat() == MediaTypeRegistry.APPLICATION_LINK_FORMAT) {

					Set<WebLink> links = LinkFormat.parse(coapResp.getResponseText());

					links.forEach(link -> {

						if(link.getURI() != null
								&& !link.getURI().equals(WELL_KNOWN_CORE_URI)
								&& link.getAttributes() != null
								&& link.getAttributes().getCount() > 0){

							//If the resource is a core.s or core.a and it is observable save the target url reference
							if(link.getAttributes().containsAttribute(OBSERVABLE_CORE_ATTRIBUTE) &&
									link.getAttributes().containsAttribute(INTERFACE_CORE_ATTRIBUTE) &&
									(link.getAttributes().getAttributeValues(INTERFACE_CORE_ATTRIBUTE).get(0).equals(CoreInterfaces.CORE_S.getValue()) || link.getAttributes().getAttributeValues(INTERFACE_CORE_ATTRIBUTE).get(0).equals(CoreInterfaces.CORE_A.getValue()))){

								boolean supportSenml = false;

								if(link.getAttributes().containsAttribute(CONTENT_TYPE_ATTRIBUTE))
									supportSenml = link.getAttributes().getAttributeValues(CONTENT_TYPE_ATTRIBUTE).contains("110");

								logger.info("Target resource found ! URI: {}} (Senml: {})", link.getURI(), supportSenml);

								//E.g. coap://<node_ip>:<node_port>/<resource_uri>
								String targetResourceUrl = String.format("coap://%s:%d%s",
										TARGET_SMART_OBJECT_ADDRESS,
										TARGET_SMART_OBJECT_IP,
										link.getURI());

								targetObservableResourceList.add(new TargetCoapResourceDescriptor(targetResourceUrl, supportSenml));

								logger.warn("Target Resource URL: {} correctly saved !", targetResourceUrl);

							}
							else
								logger.info("Resource {} does not match filtering parameters ....", link.getURI());

						}
					});

				} else {
					logger.error("CoRE Link Format Response not found !");
				}
			}
		} catch (ConnectorException | IOException e) {
			e.printStackTrace();
		}

	}
}