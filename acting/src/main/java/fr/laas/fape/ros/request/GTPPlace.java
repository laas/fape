package fr.laas.fape.ros.request;

import fr.laas.fape.ros.message.MessageFactory;
import gtp_ros_msg.requestGoal;


public class GTPPlace extends GTPRequest {

    public final String agent;
    public final String object;
    public final String support;

    public GTPPlace(String agent, String object, String surface) {
        this.agent = agent;
        this.object = object;
        this.support = surface;
    }

    @Override
    public requestGoal asActionGoal() {
        requestGoal goal = GTPRequest.emptyGoal();
        goal.getReq().setRequestType("planning");
        goal.getReq().setActionName("place");
        goal.getReq().getInvolvedAgents().add(MessageFactory.getAg("mainAgent", agent));
        goal.getReq().getInvolvedObjects().add(MessageFactory.getObj("mainObject", object));
        goal.getReq().getInvolvedObjects().add(MessageFactory.getObj("supportObject", support));
        goal.getReq().getPredecessorId().setActionId(-1);
        goal.getReq().getPredecessorId().setAlternativeId(-1);
        return goal;
    }
}
