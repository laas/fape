package fr.laas.fape.ros.action;

import fr.laas.fape.ros.MainTesting;
import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.database.Database;
import fr.laas.fape.ros.exception.ActionFailure;
import fr.laas.fape.ros.message.MessageFactory;
import fr.laas.fape.ros.request.*;
import gtp_ros_msg.Pt;

import java.util.List;
import java.util.stream.Collectors;

import static fr.laas.fape.ros.ROSUtils.dist;


public class GoToPick {

    public static double ENGAGE_DIST = 0.5;
    public static double PICK_DISTANCE = 0.65;
    public static double PLACE_DISTANCE = 0.85;
    public static int NUM_APPROACHES = 12;

    public static void goToPick(String agent, String targetObject) throws ActionFailure {
        ApproachAngle angle = getFeasibleApproach(agent, targetObject, PICK_DISTANCE);
        Pt targetObjectPose = MessageFactory.getXYYawFromPose(Database.getPoseOf(targetObject));
        if(angle == null)
            throw new ActionFailure("GoToPick: no possible approaches to "+targetObject);

        GTPUpdate.update();
        Pt preManipPose = angle.getApproachPose(targetObjectPose, PICK_DISTANCE+ENGAGE_DIST);
        MoveBaseClient.sendGoTo(preManipPose.getX(), preManipPose.getY(), preManipPose.getZ());

        engage(agent, targetObject, angle);
        GTPUpdate.update();
        GTPPick pick = new GTPPick(agent, targetObject);
        pick.send();
        pick.execute();

        disengage(agent);
    }

    public static void goToPlace(String agent, String targetObject, String targetSurface) throws ActionFailure {
        ApproachAngle angle = getFeasibleApproach(agent, targetSurface, PLACE_DISTANCE);
        Pt targetObjectPose = MessageFactory.getXYYawFromPose(Database.getPoseOf(targetSurface));
        if(angle == null)
            throw new ActionFailure("GoToPlace: no possible approaches to "+targetObject);

        GTPUpdate.update();
        Pt preManipPose = angle.getApproachPose(targetObjectPose, PLACE_DISTANCE+ENGAGE_DIST);
        MoveBaseClient.sendGoTo(preManipPose.getX(), preManipPose.getY(), preManipPose.getZ());

        engage(agent, targetObject, angle);
        GTPUpdate.update();
        GTPPlace place = new GTPPlace(agent, targetObject, targetSurface);
        place.send();
        place.execute();

        disengage(agent);
    }

    public static void engage(String agent, String targetObject, ApproachAngle approach) throws ActionFailure {
        MoveArmToQ.moveRightToManipulationPose();
        MoveArmToQ.moveLeftToManipulationPose();
        MoveBlind.moveForward(ENGAGE_DIST);
    }

    public static void disengage(String agent) throws ActionFailure {
        MoveBlind.moveBackward(ENGAGE_DIST);
    }

    public static ApproachAngle getFeasibleApproach(String agent, String targetObject, double distance) {
        Pt robotPose = MessageFactory.getXYYawFromPose(Database.getPoseOf(agent));
        Pt targetPt = MessageFactory.getXYYawFromPose(Database.getPoseOf(targetObject));

        try {
            new GTPUpdate().send();
        } catch (ActionFailure e) {
            throw new RuntimeException("Failed to send a GTP update");
        }

        // approaches sorted by increasing distance to current pose
        List<ApproachAngle> candidateApproaches = ROSUtils.approaches(NUM_APPROACHES).stream()
                .sorted((e1, e2) -> Double.compare(
                        dist(robotPose, getManipulationPose(targetPt,e1, distance)),
                        dist(robotPose, getManipulationPose(targetPt,e2, distance))))
                .collect(Collectors.toList());
        ApproachAngle bestFeasible = null;
        for(ApproachAngle a : candidateApproaches) {
            try {
                GTPNavigateTo nav = new GTPNavigateTo("PR2_ROBOT", getManipulationPose(targetPt, a, distance));
                nav.send();
                // if we got here, then GTP found a way to navigate to this point
                bestFeasible = a;
                break;
            } catch (ActionFailure e) {
                // GTP could not find a navigation plan, skip this approach angle
                continue;
            }
        }
        return bestFeasible;
    }

    public static Pt getManipulationPose(Pt target, ApproachAngle angle, double distance) {
        return angle.getApproachPose(target, distance);
    }

}
