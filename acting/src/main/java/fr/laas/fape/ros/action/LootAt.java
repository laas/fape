package fr.laas.fape.ros.action;

import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.database.Database;
import fr.laas.fape.ros.exception.ActionFailure;
import geometry_msgs.Pose;
import pr2motion.*;

public class LootAt {

    private static SimpleActionServer<Head_Move_TargetActionGoal, Head_Move_TargetActionFeedback, Head_Move_TargetActionResult> instance = null;

    public static SimpleActionServer<Head_Move_TargetActionGoal, Head_Move_TargetActionFeedback, Head_Move_TargetActionResult> getInstance() {
        if(instance == null) {
            instance = new SimpleActionServer<>("pr2motion/Head_Move_Target", Head_Move_TargetActionGoal._TYPE, Head_Move_TargetActionFeedback._TYPE, Head_Move_TargetActionResult._TYPE);
            instance.cancelPreviousGoals();
        }
        return instance;
    }

    public static Head_Move_TargetActionResult lookAt(String object) throws ActionFailure {
        Pose pose = Database.getPoseOf(object);
        try {
            Head_Move_TargetGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Head_Move_TargetGoal");
            goal.getHeadMode().setValue(0); // look at
            goal.setHeadTargetFrame("map");
            goal.setHeadTargetX(pose.getPosition().getX());
            goal.setHeadTargetY(pose.getPosition().getY());
            if(object.contains("TABLE"))
                goal.setHeadTargetZ(pose.getPosition().getZ() + 0.7);
            else
                goal.setHeadTargetZ(pose.getPosition().getZ());
            Head_Move_TargetActionResult res = getInstance().sendGoal(goal);
            if (res.getResult().getGenomSuccess()) {
                return res;
            } else {
                throw new ActionFailure("LookAt: " + res.getResult().getGenomExdetail());
            }
        } catch (ActionFailure e) {
            throw e;
        } catch (Throwable e) {
            throw new ActionFailure("LookAt: unexpected exception.", e);
        }
    }
}
