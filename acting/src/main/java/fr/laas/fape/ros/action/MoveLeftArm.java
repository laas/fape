package fr.laas.fape.ros.action;

import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.exception.ActionFailure;
import pr2motion.Arm_Left_MoveActionFeedback;
import pr2motion.Arm_Left_MoveActionGoal;
import pr2motion.Arm_Left_MoveActionResult;
import pr2motion.Arm_Left_MoveGoal;

public class MoveLeftArm {

    private static SimpleActionServer<Arm_Left_MoveActionGoal, Arm_Left_MoveActionFeedback, Arm_Left_MoveActionResult> instance = null;

    public static SimpleActionServer<Arm_Left_MoveActionGoal, Arm_Left_MoveActionFeedback, Arm_Left_MoveActionResult> getInstance() {
        if(instance == null) {
            instance = new SimpleActionServer<>("pr2motion/Arm_Left_Move", Arm_Left_MoveActionGoal._TYPE, Arm_Left_MoveActionFeedback._TYPE, Arm_Left_MoveActionResult._TYPE);
            instance.cancelPreviousGoals();
        }
        return instance;
    }

    public static Arm_Left_MoveActionResult move() throws ActionFailure {
        try {
            Arm_Left_MoveGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Arm_Left_MoveGoal");
            goal.getTrajMode().setValue(PR2Motion.TRAJ_GATECH);
            goal.getPathMode().setValue(PR2Motion.PATH_PORT);
            Arm_Left_MoveActionResult res = getInstance().sendGoal(goal);
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
