package fr.laas.fape.ros.action;

import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.database.Attachments;
import pr2motion.*;

public class GripperOperator {
    public static final int GRIPPER_OPEN = 0;
    public static final int GRIPPER_CLOSE = 1;

    private SimpleActionServer<Gripper_Left_OperateActionGoal, Gripper_Left_OperateActionFeedback, Gripper_Left_OperateActionResult> leftActioner = null;
    private SimpleActionServer<Gripper_Right_OperateActionGoal, Gripper_Right_OperateActionFeedback, Gripper_Right_OperateActionResult> rightActioner = null;

    private static GripperOperator instance = null;

    private GripperOperator() {
        leftActioner = new SimpleActionServer<>("pr2motion/Gripper_Left_Operate", Gripper_Left_OperateActionGoal._TYPE, Gripper_Left_OperateActionFeedback._TYPE, Gripper_Left_OperateActionResult._TYPE);
        rightActioner = new SimpleActionServer<>("pr2motion/Gripper_Right_Operate", Gripper_Right_OperateActionGoal._TYPE, Gripper_Right_OperateActionFeedback._TYPE, Gripper_Right_OperateActionResult._TYPE);
    }

    public static GripperOperator getInstance() {
        if(instance == null) {
            instance = new GripperOperator();
        }
        return instance;
    }

    public static boolean closeGripper(String side) {
        return getInstance().operate(side, GRIPPER_CLOSE);
    }

    public static boolean openGripper(String side) {
        return getInstance().operate(side, GRIPPER_OPEN);
    }

    private boolean operate(String side, int operation) {
        assert operation == GRIPPER_CLOSE || operation == GRIPPER_OPEN;
        if(side.equals("left")) {
            Gripper_Left_OperateGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Gripper_Left_OperateGoal");
            goal.getGoalMode().setValue(operation);
            boolean res = leftActioner.sendGoal(goal).getResult().getGenomSuccess();
            ROSUtils.sleep(12000); // magic number to wait that the gripper be close enough
            if(operation == GRIPPER_OPEN)
                Attachments.attachedObjects().stream()
                        .filter(o -> Attachments.getHoldingArm(o).equals(side))
                        .forEach(Attachments::removeAttachment);
            return res;
        } else {
            assert side.equals("right");
            Gripper_Right_OperateGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Gripper_Right_OperateGoal");
            goal.getGoalMode().setValue(operation);
            boolean res = rightActioner.sendGoal(goal).getResult().getGenomSuccess();
            ROSUtils.sleep(12000);
            if(operation == GRIPPER_OPEN)
                Attachments.attachedObjects().stream()
                        .filter(o -> Attachments.getHoldingArm(o).equals(side))
                        .forEach(Attachments::removeAttachment);
            return res;
        }
    }
}
