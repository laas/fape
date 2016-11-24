package fr.laas.fape.ros.request;


import fr.laas.fape.ros.message.MessageFactory;
import gtp_ros_msg.requestGoal;

public class GTPMoveTo extends GTPRequest {

    public final String agent;
    public final String configuration;

    public GTPMoveTo(String agent, String configuration) {
        this.agent = agent;
        this.configuration = configuration;
    }

    @Override
    public requestGoal asActionGoal() {
        requestGoal goal = GTPRequest.emptyGoal();
        goal.getReq().setRequestType("planning");
        goal.getReq().setActionName("moveTo");
        goal.getReq().getInvolvedAgents().add(MessageFactory.getAg("mainAgent", agent));
        goal.getReq().getData().add(MessageFactory.getData("confName", configuration));
        return goal;
    }

    public static GTPMoveTo rightToManipulationPose(String agent) {
//        return new GTPMoveTo(agent, "manipulationPoseRight");
        return new GTPMoveTo(agent, "manipulationPoseRight");
    }


    public static GTPMoveTo leftToManipulationPose(String agent) {
        return new GTPMoveTo(agent, "manipulationPoseLeft");
    }

    public static GTPMoveTo rightToRestPose(String agent) {
        return new GTPMoveTo(agent, "restPoseRight");
    }


    public static GTPMoveTo leftToRestPose(String agent) {
        return new GTPMoveTo(agent, "restPoseLeft");
    }
}
