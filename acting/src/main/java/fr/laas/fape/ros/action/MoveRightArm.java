package fr.laas.fape.ros.action;

import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.exception.ActionFailure;
import pr2motion.*;

public class MoveRightArm {

    private static SimpleActionServer<Arm_Right_MoveActionGoal, Arm_Right_MoveActionFeedback, Arm_Right_MoveActionResult> instance = null;

    public static SimpleActionServer<Arm_Right_MoveActionGoal, Arm_Right_MoveActionFeedback, Arm_Right_MoveActionResult> getInstance() {
        if(instance == null) {
            instance = new SimpleActionServer<>("pr2motion/Arm_Right_Move", Arm_Right_MoveActionGoal._TYPE, Arm_Right_MoveActionFeedback._TYPE, Arm_Right_MoveActionResult._TYPE);
            instance.cancelPreviousGoals();
        }
        return instance;
    }

    public static Arm_Right_MoveActionResult move() throws ActionFailure {
        try {
            Arm_Right_MoveGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Arm_Right_MoveGoal");
            goal.getTrajMode().setValue(PR2Motion.TRAJ_GATECH);
            goal.getPathMode().setValue(PR2Motion.PATH_PORT);
            Arm_Right_MoveActionResult res = getInstance().sendGoal(goal);
            if (res.getResult().getGenomSuccess()) {
                return res;
            } else {
                throw new ActionFailure("ArmRightMove: " + res.getResult().getGenomExdetail());
            }
        } catch (ActionFailure e) {
            throw e;
        } catch (Throwable e) {
            throw new ActionFailure("ArmRightMove: unexpected exception.", e);
        }
    }
}
