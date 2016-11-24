package fr.laas.fape.ros.action;


import fr.laas.fape.ros.ROSNode;
import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.database.Database;
import fr.laas.fape.ros.exception.ActionFailure;
import fr.laas.fape.ros.message.MessageFactory;
import geometry_msgs.Pose;
import geometry_msgs.Twist;
import org.ros.node.topic.Publisher;

public class MoveBlind {

    private static MoveBlind instance;

    final Publisher<Twist> publisher;
    long expirationLimit = 0;

    private MoveBlind() {
        Database.initialize();
        publisher = ROSNode.getNode().newPublisher("/navigation/cmd_vel", geometry_msgs.Twist._TYPE);
    }

    public static MoveBlind getInstance() {
        if(instance == null)
            instance = new MoveBlind();
        return instance;
    }

    public static void cancelAllGoals() {
        getInstance().expirationLimit = System.currentTimeMillis();
    }

    public static void moveForward(double distance) throws ActionFailure {
        long startTime = System.currentTimeMillis();
        Pose initPose = Database.getPoseOf("PR2_ROBOT");
        Twist cmd = ROSUtils.emptyMessageFromType("geometry_msgs/Twist");
        if(distance > 0)
            cmd.getLinear().setX(0.25); // move forward at 25cm/s
        else
            cmd.getLinear().setX(-0.25); // move backward at 25cm/s
        boolean done = false;
        while (!done && startTime > getInstance().expirationLimit) {
            getInstance().publisher.publish(cmd);
            ROSUtils.sleep(10);
            Pose cur = Database.getPoseOf("PR2_ROBOT");
            double traveledDist = Math.sqrt(
                    Math.pow(initPose.getPosition().getX() - cur.getPosition().getX(), 2) +
                            Math.pow(initPose.getPosition().getY() - cur.getPosition().getY(), 2));
            if(traveledDist >= Math.abs(distance))
                done = true;
        }
    }

    public static void turnTowards(double angle) throws ActionFailure {
        long startTime = System.currentTimeMillis();

        while (startTime > getInstance().expirationLimit) {
            double current = MessageFactory.getXYYawFromPose(Database.getPoseOf("PR2_ROBOT")).getZ();
            Twist cmd = ROSUtils.emptyMessageFromType("geometry_msgs/Twist");
            double diff = angle - current;
            if(Math.abs(diff) < 0.1)
                break; // target angle reached
            double speed;
            if(Math.abs(diff) < 0.3)
                speed = 0.4;
            else
                speed = 1;

            if (angle > 0)
                cmd.getAngular().setZ(speed);
            else
                cmd.getAngular().setZ(-speed);
            getInstance().publisher.publish(cmd);
            ROSUtils.sleep(10);
        }
    }

    public static void moveBackward(double distance) throws ActionFailure {
        moveForward(-distance);
    }
}
