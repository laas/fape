package fr.laas.fape.ros.request;

import gtp_ros_msg.requestGoal;

/**
 * Created by abitmonn on 11/18/16.
 */
public class GTPDetails extends GTPRequest {

    public final long actionID;
    public final long alternativeID;

    public GTPDetails(long actionID) {
        this.actionID = actionID;
        this.alternativeID = 0;
    }

    @Override
    public requestGoal asActionGoal() {
        requestGoal g = GTPRequest.emptyGoal();
        g.getReq().setRequestType("details");
        g.getReq().getLoadAction().setActionId(actionID);
        g.getReq().getLoadAction().setAlternativeId(alternativeID);
        return g;
    }
}
