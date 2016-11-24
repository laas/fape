package fr.laas.fape.ros.request;

import gtp_ros_msg.requestGoal;


public class GTPAttachFromTask extends GTPRequest {

    public final long actionID, alternativeID;

    public GTPAttachFromTask(long actionID) {
        this.actionID = actionID;
        this.alternativeID = 0;
    }

    @Override
    public requestGoal asActionGoal() {
        requestGoal g = GTPRequest.emptyGoal();
        g.getReq().setRequestType("addAttachemnt"); // TYPO is not a bug, its a feature...
        g.getReq().getLoadAction().setActionId(actionID);
        g.getReq().getLoadAction().setAlternativeId(alternativeID);
        return g;
    }
}
