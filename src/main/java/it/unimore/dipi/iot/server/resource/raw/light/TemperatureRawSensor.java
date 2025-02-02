package it.unimore.dipi.iot.server.resource.raw.light;

import it.unimore.dipi.iot.server.resource.raw.ResourceDataListener;
import it.unimore.dipi.iot.server.resource.raw.SmartObjectResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.lang.Math;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smarthome
 * @created 11/11/2020 - 14:43
 */
public class TemperatureRawSensor extends SmartObjectResource<Double> {

    private static Logger logger = LoggerFactory.getLogger(TemperatureRawSensor.class);

    private static final double MIN_TEMPERATURE_VALUE = 25.0;

    private static final double MAX_TEMPERATURE_VALUE = 100.0;

    private static final double MIN_TEMPERATURE_VARIATION = 0.1;

    private static final double MAX_TEMPERATURE_VARIATION = 5.0;

    private static final String LOG_DISPLAY_NAME = "TemperatureSensor";

    //Ms associated to data update
    public static final long UPDATE_PERIOD = 5000;

    private static final long TASK_DELAY_TIME = 5000;

    private static final String RESOURCE_TYPE = "iot.sensor.temperature";

    private Double updatedValue;

    private Random random;

    private Timer updateTimer = null;
    private boolean isActive = true;

    public TemperatureRawSensor() {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        init();
    }

    private void init(){

        try{

            this.random = new Random(System.currentTimeMillis());
            this.updatedValue = MIN_TEMPERATURE_VALUE + this.random.nextDouble()*(MAX_TEMPERATURE_VALUE - MIN_TEMPERATURE_VALUE);

            startPeriodicEventValueUpdateTask();

        }catch (Exception e){
            logger.error("Error initializing the IoT Resource ! Msg: {}", e.getLocalizedMessage());
        }

    }

    private void startPeriodicEventValueUpdateTask(){

        try{

            logger.info("Starting periodic Update Task with Period: {} ms", UPDATE_PERIOD);

            this.updateTimer = new Timer();
            this.updateTimer.schedule(new TimerTask() {
                @Override
                public void run() {

                    if(isActive) {
                        random = new Random(System.currentTimeMillis());
                        updatedValue = MIN_TEMPERATURE_VALUE + random.nextDouble()*(MAX_TEMPERATURE_VALUE - MIN_TEMPERATURE_VALUE);
//                        double variation = (MIN_TEMPERATURE_VARIATION + MAX_TEMPERATURE_VARIATION * random.nextDouble()) * (random.nextDouble() > 0.5 ? 1.0 : -1.0);
//                        updatedValue = Math.abs(updatedValue + variation);
                        notifyUpdate(updatedValue);

                    }
                    else {
                        notifyUpdate(0.0);
                    }




                }
            }, TASK_DELAY_TIME, UPDATE_PERIOD);

        }catch (Exception e){
            logger.error("Error executing periodic resource value ! Msg: {}", e.getLocalizedMessage());
        }

    }

    @Override
    public Double loadUpdatedValue() {
        return this.updatedValue;
    }
    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean active) {
        isActive = active;
    }

    public static void main(String[] args) {

        TemperatureRawSensor rawResource = new TemperatureRawSensor();
        logger.info("New {} Resource Created with Id: {} ! {} New Value: {}",
                rawResource.getType(),
                rawResource.getId(),
                LOG_DISPLAY_NAME,
                rawResource.loadUpdatedValue());

        rawResource.addDataListener(new ResourceDataListener<Double>() {
            @Override
            public void onDataChanged(SmartObjectResource<Double> resource, Double updatedValue) {

                if(resource != null && updatedValue != null)
                    logger.info("Device: {} -> New Value Received: {}", resource.getId(), updatedValue);
                else
                    logger.error("onDataChanged Callback -> Null Resource or Updated Value !");
            }
        });

    }

}
