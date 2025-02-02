package it.unimore.dipi.iot.server.resource.raw.light;

import it.unimore.dipi.iot.server.resource.raw.ResourceDataListener;
import it.unimore.dipi.iot.server.resource.raw.SmartObjectResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smarthome
 * @created 11/11/2020 - 15:07
 */
public class ColorChangerRawActuator extends SmartObjectResource<String> {

    private static Logger logger = LoggerFactory.getLogger(ColorChangerRawActuator.class);

    private static final String LOG_DISPLAY_NAME = "ColorChangerActuator";

    private static final String RESOURCE_TYPE = "iot.actuator.color";

    private String color;
    private List<String> colorsList = Arrays.asList("white", "red", "blue", "green");

    public List<String> getColorsList() {
        return colorsList;
    }

    public ColorChangerRawActuator() {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.color = "white";
    }

    public String getColor() {
        return color;
    }

    public void setColor(String chosen_color) {
        color = chosen_color;
        notifyUpdate(color);
    }
    public void incrementColor(){
        if (this.color != null){
            int i = colorsList.indexOf(this.color);
            this.color = colorsList.get((i+1)% colorsList.size());
            notifyUpdate(this.color);
        }
    }


    @Override
    public String loadUpdatedValue() {
        return this.color;
    }

    public static void main(String[] args) {

        ColorChangerRawActuator rawResource = new ColorChangerRawActuator();
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
                        rawResource.setColor(rawResource.loadUpdatedValue());
                        Thread.sleep(1000);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        rawResource.addDataListener(new ResourceDataListener<String>() {
            @Override
            public void onDataChanged(SmartObjectResource<String> resource, String updatedValue) {

                if(resource != null && updatedValue != null)
                    logger.info("Device: {} -> New Value Received: {}", resource.getId(), updatedValue);
                else
                    logger.error("onDataChanged Callback -> Null Resource or Updated Value !");
            }
        });

    }

}
