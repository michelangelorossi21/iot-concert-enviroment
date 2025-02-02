package it.unimore.dipi.iot.server.resource.raw.instrument;

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
public class VolumeRawActuator extends SmartObjectResource<Integer> {

    private static Logger logger = LoggerFactory.getLogger(VolumeRawActuator.class);

    private static final String LOG_DISPLAY_NAME = "VolumeActuator";

    private static final String RESOURCE_TYPE = "iot.actuator.volume";

    private int volume;

    public VolumeRawActuator() {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.volume = 10;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(int chosen_volume) {
        volume = chosen_volume;
        notifyUpdate(volume);
    }
    public void increaseVolume() {
        this.volume = (this.volume + 10) % 100;
        notifyUpdate(this.volume);
    }

    @Override
    public Integer loadUpdatedValue() {
        return this.volume;
    }

    public static void main(String[] args) {

        VolumeRawActuator rawResource = new VolumeRawActuator();
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
                        rawResource.setVolume(rawResource.loadUpdatedValue());
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
