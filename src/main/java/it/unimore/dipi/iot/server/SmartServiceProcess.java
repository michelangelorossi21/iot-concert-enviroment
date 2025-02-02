package it.unimore.dipi.iot.server;

import it.unimore.dipi.iot.server.resource.coap.*;
import it.unimore.dipi.iot.server.resource.raw.*;
import it.unimore.dipi.iot.server.resource.raw.instrument.AcousticRawSensor;
import it.unimore.dipi.iot.server.resource.raw.instrument.VolumeRawActuator;
import it.unimore.dipi.iot.server.resource.raw.light.BrightnessRawActuator;
import it.unimore.dipi.iot.server.resource.raw.light.ColorChangerRawActuator;
import it.unimore.dipi.iot.server.resource.raw.light.TemperatureRawSensor;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smarthome
 * @created 11/11/2020 - 15:39
 */
public class SmartServiceProcess extends CoapServer {

    private final static Logger logger = LoggerFactory.getLogger(SmartServiceProcess.class);
    private static final int COAP_ENDPOINT = 5684;
    private static final List<String> instruments = Arrays.asList("keyboard", "guitar");
    private static final List<String> lights = Arrays.asList("main_light", "backstage_light");

    public SmartServiceProcess(int port) {

        // Create different instruments and lights:
        super(port);
        for (int i=0; i < instruments.size(); i++) {
            String deviceId_instrument = String.format("dipi:iot:%s", UUID.randomUUID().toString());
            this.add(createInstrumentResource(deviceId_instrument, instruments.get(i)));
        }

        for (int i=0; i < lights.size(); i++) {
            String deviceId_light = String.format("dipi:iot:%s", UUID.randomUUID().toString());
            this.add(createLightResource(deviceId_light, lights.get(i)));
        }
    }

    private CoapResource createLightResource(String deviceId, String name){

        CoapResource lightRootResource = new CoapResource(name);

        //INIT Emulated Physical Sensors and Actuators
        TemperatureRawSensor lightTemperatureRawSensor = new TemperatureRawSensor();
        EnergyRawSensor lightEnergyRawSensor = new EnergyRawSensor();
        SwitchRawActuator lightSwitchRawActuator = new SwitchRawActuator();
        BrightnessRawActuator lightBrightnessRawActuator = new BrightnessRawActuator();
        ColorChangerRawActuator lightColorChangerRawActuator = new ColorChangerRawActuator();

        //Light Resources
        CoapTemperatureResource lightTemperatureSensorResource = new CoapTemperatureResource(deviceId, "temperature", lightTemperatureRawSensor);
        CoapEnergyConsumptionResource lightEnergyResource = new CoapEnergyConsumptionResource(deviceId, "energy", lightEnergyRawSensor);
        CoapSwitchActuatorResource lightSwitchResource = new CoapSwitchActuatorResource(deviceId, "switch", lightSwitchRawActuator);
        CoapBrightnessActuatorResource lightBrightnessResource = new CoapBrightnessActuatorResource(deviceId, "brightness", lightBrightnessRawActuator);
        CoapColorActuatorResource lightColorResource = new CoapColorActuatorResource(deviceId, "color", lightColorChangerRawActuator);

        lightRootResource.add(lightTemperatureSensorResource);
        lightRootResource.add(lightEnergyResource);
        lightRootResource.add(lightSwitchResource);
        lightRootResource.add(lightBrightnessResource);
        lightRootResource.add(lightColorResource);

        //Handle Emulated Resource notification
        lightSwitchRawActuator.addDataListener(new ResourceDataListener<Boolean>() {
            @Override
            public void onDataChanged(SmartObjectResource<Boolean> resource, Boolean updatedValue) {
                logger.info("[LIGHT-BEHAVIOUR] -> Updated Switch Value: {}", updatedValue);
                logger.info("[LIGHT-BEHAVIOUR] -> Updating energy sensor configuration ...");
                lightEnergyRawSensor.setActive(updatedValue);
                lightTemperatureRawSensor.setActive(updatedValue);

            }
        });

        return lightRootResource;
    }

    private CoapResource createInstrumentResource(String deviceId, String name){

        CoapResource instrumentRootResource = new CoapResource(name);

        //INIT Emulated Physical Sensors and Actuators
        AcousticRawSensor instrumentAcousticRawSensor = new AcousticRawSensor();
        EnergyRawSensor instrumentEnergyRawSensor = new EnergyRawSensor();
        SwitchRawActuator instrumentSwitchRawActuator = new SwitchRawActuator();
        VolumeRawActuator instrumentVolumeRawActuator = new VolumeRawActuator();

        //Instrument Resource
        CoapAcousticSensorResource instrumentAcousticSensorResource = new CoapAcousticSensorResource(deviceId, "acoustic", instrumentAcousticRawSensor);
        CoapEnergyConsumptionResource instrumentEnergyResource = new CoapEnergyConsumptionResource(deviceId, "energy", instrumentEnergyRawSensor);
        CoapSwitchActuatorResource instrumentSwitchResource = new CoapSwitchActuatorResource(deviceId, "switch", instrumentSwitchRawActuator);
        CoapVolumeActuatorResource instrumentVolumeResource = new CoapVolumeActuatorResource(deviceId, "volume", instrumentVolumeRawActuator);

        instrumentRootResource.add(instrumentAcousticSensorResource);
        instrumentRootResource.add(instrumentEnergyResource);
        instrumentRootResource.add(instrumentSwitchResource);
        instrumentRootResource.add(instrumentVolumeResource);

        //Handle Emulated Resource notification
        instrumentSwitchRawActuator.addDataListener(new ResourceDataListener<Boolean>() {
            @Override
            public void onDataChanged(SmartObjectResource<Boolean> resource, Boolean updatedValue) {
                logger.info("[INSTRUMENT-BEHAVIOUR] -> Updated Switch Value: {}", updatedValue);
                logger.info("[INSTRUMENT-BEHAVIOUR] -> Updating energy sensor configuration ...");
                instrumentEnergyRawSensor.setActive(updatedValue);
                instrumentAcousticRawSensor.setActive(updatedValue);
            }
        });

        return instrumentRootResource;
    }

    public static void main(String[] args) {

        SmartServiceProcess smartServiceProcess = new SmartServiceProcess(COAP_ENDPOINT);
        smartServiceProcess.start();

        logger.info("Coap Server Started ! Available resources: ");

        smartServiceProcess.getRoot().getChildren().stream().forEach(resource -> {
            logger.info("Resource {} -> URI: {} (Observable: {})", resource.getName(), resource.getURI(), resource.isObservable());
            if(!resource.getURI().equals("/.well-known")){
                resource.getChildren().stream().forEach(childResource -> {
                    logger.info("\t Resource {} -> URI: {} (Observable: {})", childResource.getName(), childResource.getURI(), childResource.isObservable());
                });
            }
        });

    }

}
