package fr.laas.fape.ros.action;


import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.exception.ActionFailure;
import pr2motion.*;

public class MoveArmToQ {

    private  SimpleActionServer<Arm_Left_MoveToQGoalActionGoal, Arm_Left_MoveToQGoalActionFeedback, Arm_Left_MoveToQGoalActionResult> left =null;
    private  SimpleActionServer<Arm_Right_MoveToQGoalActionGoal, Arm_Right_MoveToQGoalActionFeedback, Arm_Right_MoveToQGoalActionResult> right =null;

    private static MoveArmToQ instance = null;

    public static MoveArmToQ getInstance() {
        if (instance == null)
            instance = new MoveArmToQ();
        return instance;
    }

    private MoveArmToQ() {
        left = new SimpleActionServer<>("pr2motion/Arm_Left_MoveToQGoal", Arm_Left_MoveToQGoalActionGoal._TYPE, Arm_Left_MoveToQGoalActionFeedback._TYPE, Arm_Left_MoveToQGoalActionResult._TYPE);
        right = new SimpleActionServer<>("pr2motion/Arm_Right_MoveToQGoal", Arm_Right_MoveToQGoalActionGoal._TYPE, Arm_Right_MoveToQGoalActionFeedback._TYPE, Arm_Right_MoveToQGoalActionResult._TYPE);
        left.cancelPreviousGoals();
        right.cancelPreviousGoals();
    }

    private boolean moveToQ(String arm, double a1, double a2, double a3, double a4, double a5, double a6, double a7) throws ActionFailure {
        if(arm.equals("left")) {
            Arm_Left_MoveToQGoalGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Arm_Left_MoveToQGoalGoal");
            goal.getTrajMode().setValue(PR2Motion.TRAJ_GATECH);
            goal.setShoulderPanJoint(a1);
            goal.setShoulderLiftJoint(a2);
            goal.setUpperArmRollJoint(a3);
            goal.setElbowFlexJoint(a4);
            goal.setForearmRollJoint(a5);
            goal.setWristFlexJoint(a6);
            goal.setWristRollJoint(a7);
            Arm_Left_MoveToQGoalActionResult res = left.sendGoal(goal);
            if(res.getResult().getGenomSuccess())
                return true;
            else
                throw new ActionFailure("MoveLeftToQ: "+res.getResult().getGenomExdetail());
        } else {
            Arm_Right_MoveToQGoalGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Arm_Right_MoveToQGoalGoal");
            goal.getTrajMode().setValue(PR2Motion.TRAJ_GATECH);
            goal.setShoulderPanJoint(a1);
            goal.setShoulderLiftJoint(a2);
            goal.setUpperArmRollJoint(a3);
            goal.setElbowFlexJoint(a4);
            goal.setForearmRollJoint(a5);
            goal.setWristFlexJoint(a6);
            goal.setWristRollJoint(a7);
            Arm_Right_MoveToQGoalActionResult res = right.sendGoal(goal);
            if (res.getResult().getGenomSuccess())
                return true;
            else
                throw new ActionFailure("MoveRightToQ: " + res.getResult().getGenomExdetail());
        }
    }

    public static boolean moveRightToManipulationPose() throws ActionFailure {
        return getInstance().moveToQ("right", -1.952888, -0.095935, -0.601572, -1.600124, 0.018247, -0.432897, -1.730082);
    }

    public static boolean moveLeftToManipulationPose() throws ActionFailure {
        return getInstance().moveToQ("left", 1.91155, -0.0984492, 0.6, -1.6534, -0.02173, -0.473717, -1.76561);
    }

    public static boolean moveRightToRestPose() throws ActionFailure {
        return getInstance().moveToQ("right", -2.135282, 0.967001, -1.810648, -1.878228, -0.697032, -0.433332, -1.729122);
    }



    public static boolean moveLeftToRestPose() throws ActionFailure {
        return getInstance().moveToQ("left", 2.135086, 0.946210, 1.746768, -1.874175, 0.826876, -0.095290, -1.764006);
    }

    public static boolean moveRightToNavigationPose() throws ActionFailure {
        return getInstance().moveToQ("right", 0, 1.39, 0, -2.32, 3.14, -0.78, 0);
    }

    public static boolean moveLeftToNavigationPose() throws ActionFailure {
        return getInstance().moveToQ("left", 0, 1.39, 0, -2.32, 3.14, -0.78, 0);
    }

    public static boolean moveLeftStraight() throws ActionFailure {
        return getInstance().moveToQ("left", 0, 0, 0, 0, 0, 0, 0);
    }
}







//pr2motion_TRAJ_MODE traj_mode
//float64 shoulder_pan_joint
//float64 shoulder_lift_joint
//float64 upper_arm_roll_joint
//float64 elbow_flex_joint
//float64 forearm_roll_joint
//float64 wrist_flex_joint
//float64 wrist_roll_joint