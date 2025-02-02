package it.unimore.dipi.iot.server.resource.raw.light;

import it.unimore.dipi.iot.server.resource.raw.ResourceDataListener;
import it.unimore.dipi.iot.server.resource.raw.SmartObjectResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smarthome
 * @created 11/11/2020 - 15:07
 */
public class BrightnessRawActuator extends SmartObjectResource<Integer> {

    private static Logger logger = LoggerFactory.getLogger(ColorChangerRawActuator.class);

    private static final String LOG_DISPLAY_NAME = "BrightnessActuator";

    private static final String RESOURCE_TYPE = "iot.actuator.brightness";

    private int bright;

    public BrightnessRawActuator() {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.bright = 10;
    }

    public Integer getBrightness() {
        return bright;
    }

    public void setBrightness(Integer chosen_bright) {
        bright = chosen_bright;
        notifyUpdate(bright);
    }

    public void incrementBrightness()  {
        this.bright = (this.bright + 10) % 100;
        notifyUpdate(this.bright);
    }

    @Override
    public Integer loadUpdatedValue() {
        return this.bright;
    }

    public static void main(String[] args) {

        BrightnessRawActuator rawResource = new BrightnessRawActuator();
        logger.info("New {} Resource Created with Id: {} ! {} New Value: {}",
                rawResource.getType(),
                rawResource.getId(),
                LOG_DISPLAY_NAME,
                rawResource.loadUpdatedValue());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    for(int i=0; i<100; i++){
                        rawResource.setBrightness(rawResource.loadUpdatedValue());
                        Thread.sleep(1000);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        rawResource.addDataListener(new ResourceDataListener<Integer>() {
            @Override
            public void onDataChanged(SmartObjectResource<Integer> resource, Integer updatedValue) {

                if(resource != null && updatedValue != null)
                    logger.info("Device: {} -> New Value Received: {}", resource.getId(), updatedValue);
                else
                    logger.error("onDataChanged Callback -> Null Resource or Updated Value !");
            }
        });

    }

}
