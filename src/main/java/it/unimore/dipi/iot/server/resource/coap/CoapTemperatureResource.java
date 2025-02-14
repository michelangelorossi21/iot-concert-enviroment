package it.unimore.dipi.iot.server.resource.coap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.server.resource.raw.ResourceDataListener;
import it.unimore.dipi.iot.server.resource.raw.SmartObjectResource;
import it.unimore.dipi.iot.server.resource.raw.light.TemperatureRawSensor;
import it.unimore.dipi.iot.utils.CoreInterfaces;
import it.unimore.dipi.iot.utils.SenMLPack;
import it.unimore.dipi.iot.utils.SenMLRecord;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smarthome
 * @created 11/11/2020 - 15:22
 */
public class CoapTemperatureResource extends CoapResource {

    private final static Logger logger = LoggerFactory.getLogger(CoapTemperatureResource.class);

    private static final String OBJECT_TITLE = "TemperatureSensor";

    private static final Number SENSOR_VERSION = 0.1;

    //Resource Unit according to SenML Units Registry (http://www.iana.org/assignments/senml/senml.xhtml)
    private String UNIT = "Cel";

    private TemperatureRawSensor temperatureRawSensor;

    private ObjectMapper objectMapper;

    private Double updatedTemperatureValue = 0.0;

    private String deviceId;

    public CoapTemperatureResource(String deviceId, String name, TemperatureRawSensor temperatureRawSensor) {

        super(name);

        if(temperatureRawSensor != null && deviceId != null){

            this.deviceId = deviceId;

            this.temperatureRawSensor = temperatureRawSensor;

            //Jackson Object Mapper + Ignore Null Fields in order to properly generate the SenML Payload
            this.objectMapper = new ObjectMapper();
            this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            setObservable(true); // enable observing
            setObserveType(CoAP.Type.CON); // configure the notification type to CONs

            getAttributes().setTitle(OBJECT_TITLE);
            getAttributes().setObservable();
            getAttributes().addAttribute("rt", temperatureRawSensor.getType());
            getAttributes().addAttribute("if", CoreInterfaces.CORE_S.getValue());
            getAttributes().addAttribute("ct", Integer.toString(MediaTypeRegistry.APPLICATION_SENML_JSON));
            getAttributes().addAttribute("ct", Integer.toString(MediaTypeRegistry.TEXT_PLAIN));
        }
        else
            logger.error("Error -> NULL Raw Reference !");

        this.temperatureRawSensor.addDataListener(new ResourceDataListener<Double>() {
            @Override
            public void onDataChanged(SmartObjectResource<Double> resource, Double updatedValue) {
                updatedTemperatureValue = updatedValue;
                changed();
            }
        });

    }

    /**
     * Create the SenML Response with the updated value and the resource information
     * @return
     */
    private Optional<String> getJsonSenmlResponse(){

        try{

            SenMLPack senMLPack = new SenMLPack();

            SenMLRecord senMLRecord = new SenMLRecord();
            senMLRecord.setBn(String.format("%s:%s", this.deviceId, this.getName()));
            senMLRecord.setBver(SENSOR_VERSION);
            senMLRecord.setU(UNIT);
            senMLRecord.setV(updatedTemperatureValue);
            senMLRecord.setT(System.currentTimeMillis());

            senMLPack.add(senMLRecord);

            return Optional.of(this.objectMapper.writeValueAsString(senMLPack));

        }catch (Exception e){
            return Optional.empty();
        }
    }

    @Override
    public void handleGET(CoapExchange exchange) {

        // the Max-Age value should match the update interval
        exchange.setMaxAge(TemperatureRawSensor.UPDATE_PERIOD);

        //If the request specify the MediaType as JSON or JSON+SenML
        if(exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_SENML_JSON ||
                exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_JSON){

            Optional<String> senmlPayload = getJsonSenmlResponse();

            if(senmlPayload.isPresent())
                exchange.respond(CoAP.ResponseCode.CONTENT, senmlPayload.get(), exchange.getRequestOptions().getAccept());
            else
                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
        //Otherwise respond with the default textplain payload
        else
            exchange.respond(CoAP.ResponseCode.CONTENT, String.valueOf(updatedTemperatureValue), MediaTypeRegistry.TEXT_PLAIN);

    }
}
