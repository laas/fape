package fr.laas.fape.ros.request;

import fr.laas.fape.ros.GTP;
import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.action.GripperOperator;
import fr.laas.fape.ros.action.MoveLeftArm;
import fr.laas.fape.ros.action.MoveRightArm;
import fr.laas.fape.ros.database.Attachments;
import fr.laas.fape.ros.exception.ActionFailure;
import gtp_ros_msg.*;

public abstract class GTPRequest {

    public requestResult result = null;
    public requestResult details = null;

    private long previousActionID = -1;
    private long previousAlternativeID = 0;

    public long getPreviousActionID() {
        return previousActionID;
    }

    public void setPreviousActionID(long previousActionID) {
        this.previousActionID = previousActionID;
    }

    public long getPreviousAlternativeID() {
        return previousAlternativeID;
    }

    public void setPreviousAlternativeID(int previousAlternativeID) {
        this.previousAlternativeID = previousAlternativeID;
    }

    public abstract requestGoal asActionGoal();

    static requestGoal emptyGoal() {
        return ROSUtils.emptyMessageFromType(requestGoal._TYPE);
    }

    public boolean send() throws ActionFailure {
        assert result == null : "Action has already been executed.";
        // attach all objects
        if(!(this instanceof GTPAttachFromTask))
            for(String attachedObject : Attachments.attachedObjects())
                new GTPAttachFromTask(Attachments.getPickID(attachedObject)).send();

        result = GTP.getInstance().sendRequest(this);
        if(result.getAns().getSuccess())
            return true;
        else
            throw new ActionFailure("Failure while planning the action \""+this+"\"");
    }

    public long getActionID() {
        assert result != null : "The action has not been executed yet.";
        return result.getAns().getIdentifier().getActionId();
    }

    public void getDetails() {
        if(details == null) {
            GTPDetails req = new GTPDetails(getActionID());
            try {
                req.send();
            } catch (ActionFailure e) {
                throw new RuntimeException("GetDetails failure", e);
            }
            details = req.result;
        }
    }

    public boolean success() {
        assert result != null : "This action was not sent";
        return result.getAns().getSuccess();
    }

    public void executeTrajectory(int subTrajID) throws ActionFailure {
        getDetails();
        // load request to pr2motion buffer
        load(subTrajID);
        long armID = details.getAns().getSubTrajs().get(subTrajID).getArmId();
        if(armID == 0)
            MoveRightArm.move();
        else
            MoveLeftArm.move();
    }

    public void execute() throws ActionFailure {
        getDetails();
        for(SubTraj subTraj : details.getAns().getSubTrajs()) {
            String arm;
            if(subTraj.getArmId() == 0)
                arm = "right";
            else
                arm = "left";
            if(details.getAns().getActionName().equals("pick") && subTraj.getSubTrajId() == 0)
                GripperOperator.openGripper(arm);
            if(subTraj.getSubTrajName().equals("grasp")) {
                GripperOperator.closeGripper(arm);
                Attachments.setAttachment(((GTPPick) this).object, arm, result.getAns().getIdentifier().getActionId());
            } else if(subTraj.getSubTrajName().equals("release")) {
                GripperOperator.openGripper("arm");
            } else {
                executeTrajectory((int) subTraj.getSubTrajId());
            }
        }
    }

    public void load(long subtrajID) throws ActionFailure {
        assert details != null;
        assert subtrajID < details.getAns().getSubTrajs().size();
        GTPLoad load = new GTPLoad(getActionID(), subtrajID);
        load.send();
    }
}
