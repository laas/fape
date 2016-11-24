package fr.laas.fape.ros.request;

import fr.laas.fape.ros.message.MessageFactory;
import gtp_ros_msg.Pt;
import gtp_ros_msg.requestGoal;


public class GTPNavigateTo extends GTPRequest {

    public final String agent;
    public final String target;
    public final Double x, y, theta;

    public GTPNavigateTo(String agent, String target) {
        this.agent = agent;
        this.target = target;
        x = null; y=null; theta =null;
    }

    public GTPNavigateTo(String agent, double x, double y, double theta) {
        this.agent = agent;
        this.target = null;
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    public GTPNavigateTo(String agent, Pt point) {
        this.agent = agent;
        this.target = null;
        this.x = point.getX();
        this.y = point.getY();
        this.theta = point.getZ();
    }

    @Override
    public requestGoal asActionGoal() {
        assert target != null;
        requestGoal goal = GTPRequest.emptyGoal();
        goal.getReq().setRequestType("planning");
        goal.getReq().setActionName("navigateTo");
        goal.getReq().getInvolvedAgents().add(MessageFactory.getAg("mainAgent", agent));
        if(target != null) {
            goal.getReq().getInvolvedObjects().add(MessageFactory.getObj("target", target));
        } else {
            goal.getReq().getPoints().add(MessageFactory.getRoledPoint("target", x, y, theta));
        }
        goal.getReq().getPredecessorId().setActionId(-1);
        goal.getReq().getPredecessorId().setAlternativeId(-1);
        return goal;
    }
}
