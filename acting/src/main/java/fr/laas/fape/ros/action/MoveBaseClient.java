package fr.laas.fape.ros.action;

import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.exception.ActionFailure;
import fr.laas.fape.ros.message.MessageFactory;
import geometry_msgs.Pose;
import move_base_msgs.*;
import fr.laas.fape.ros.sensing.CourseCorrection;

public class MoveBaseClient {

    private static final String GTP_ROS_SERVER = "/gtp_ros_server";
    private static MoveBaseClient instance = null;

    private SimpleActionServer<MoveBaseActionGoal, MoveBaseFeedback, MoveBaseActionResult> client = null;

    private MoveBaseClient() {
        client = new SimpleActionServer<>("move_base", MoveBaseActionGoal._TYPE, MoveBaseActionFeedback._TYPE, MoveBaseActionResult._TYPE);
    }

    public static MoveBaseClient getInstance() {
        if(instance == null) {
            instance = new MoveBaseClient();
        }
        return instance;
    }

    public static void cancelAllGoals() {
        getInstance().client.cancelPreviousGoals();
    }

    public static MoveBaseResult sendGoTo(Pose pose) throws ActionFailure {
        CourseCorrection.setSlowCorrection(); // slow down course correction since it makes the navigation jumpy
        try {
            MoveBaseActionGoal g = getInstance().client.newGoalMessage();
            g.getGoal().getTargetPose().getHeader().setFrameId("map");
            g.getGoal().getTargetPose().setPose(pose);
            MoveBaseActionResult res = getInstance().client.sendRequest(g);
            if(res.getResult() != null)
                return res.getResult();
            else
                throw new ActionFailure("MoveBase: "+res.getStatus().getText());

        } catch (ActionFailure e) {
            throw e;
        } catch (Throwable e) {
            throw new ActionFailure("MoveBase: unexpected exception", e);
        } finally {
            CourseCorrection.setFastCorrection();
        }
    }

    public static MoveBaseResult sendGoTo(double x, double y, double yaw) throws ActionFailure {
        Pose pose = ROSUtils.emptyMessageFromType("geometry_msgs/Pose");
        pose.getPosition().setX(x);
        pose.getPosition().setY(y);
        pose.setOrientation(MessageFactory.quaternionFromYaw(yaw));

        MoveBaseResult res = sendGoTo(pose);
        if(res != null)
            return res;
        else
            throw new ActionFailure("GoTo: action returned null");
    }
}
