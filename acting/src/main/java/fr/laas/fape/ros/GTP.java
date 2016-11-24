package fr.laas.fape.ros;

import fr.laas.fape.ros.action.SimpleActionServer;
import fr.laas.fape.ros.request.GTPRequest;
import gtp_ros_msg.*;

public class GTP  {

    private static final String GTP_ROS_SERVER = "/gtp_ros_server";
    private static GTP instance = null;

    private SimpleActionServer<requestActionGoal, requestActionFeedback, requestActionResult> client = null;

    private GTP() {
        client = new SimpleActionServer<>(GTP_ROS_SERVER, requestActionGoal._TYPE, requestActionFeedback._TYPE, requestActionResult._TYPE);
    }

    public static GTP getInstance() {
        if(instance == null) {
            instance = new GTP();
        }
        return instance;
    }

    public requestResult sendRequest(GTPRequest req) {
        requestGoal g = req.asActionGoal();
        g.getReq().getPredecessorId().setActionId(req.getPreviousActionID());
        g.getReq().getPredecessorId().setAlternativeId(req.getPreviousAlternativeID());
        return client.sendGoal(req.asActionGoal()).getResult();
    }
}
