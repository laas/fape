package fr.laas.fape.ros.request;

import gtp_ros_msg.requestGoal;


public class GTPLoad extends GTPRequest {

    public final long actionID, alternativeID, subTrajID;

    public GTPLoad(long actionID, long subTrajID) {
        this.actionID = actionID;
        this.alternativeID = 0;
        this.subTrajID = subTrajID;
    }

    @Override
    public requestGoal asActionGoal() {
        requestGoal g = GTPRequest.emptyGoal();
        g.getReq().setRequestType("load");
        g.getReq().getLoadAction().setActionId(actionID);
        g.getReq().getLoadAction().setAlternativeId(alternativeID);
        g.getReq().setLoadSubTraj(subTrajID);
        return g;
    }
}
