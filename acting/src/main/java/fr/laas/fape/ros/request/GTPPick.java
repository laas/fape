package fr.laas.fape.ros.request;

import fr.laas.fape.ros.message.MessageFactory;
import gtp_ros_msg.requestGoal;


public class GTPPick extends GTPRequest {

    public final String agent;
    public final String object;

    public GTPPick(String agent, String object) {
        this.agent = agent;
        this.object = object;
    }

    @Override
    public requestGoal asActionGoal() {
        requestGoal goal = GTPRequest.emptyGoal();
        goal.getReq().setRequestType("planning");
        goal.getReq().setActionName("pick");
        goal.getReq().getInvolvedAgents().add(MessageFactory.getAg("mainAgent", agent));
        goal.getReq().getInvolvedObjects().add(MessageFactory.getObj("mainObject", object));
        goal.getReq().getPredecessorId().setActionId(-1);
        goal.getReq().getPredecessorId().setAlternativeId(-1);
        return goal;
    }
}
