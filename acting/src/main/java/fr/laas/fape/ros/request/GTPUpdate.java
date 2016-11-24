package fr.laas.fape.ros.request;


import fr.laas.fape.ros.exception.ActionFailure;
import gtp_ros_msg.requestGoal;

public class GTPUpdate extends GTPRequest {

    @Override
    public requestGoal asActionGoal() {
        requestGoal g = GTPRequest.emptyGoal();
        g.getReq().setRequestType("update");
        return g;
    }

    public static void update() {
        try {
            new GTPUpdate().send();
        } catch (ActionFailure fail) {
            throw new RuntimeException("GTPUpdate failure", fail);
        }
    }
}
