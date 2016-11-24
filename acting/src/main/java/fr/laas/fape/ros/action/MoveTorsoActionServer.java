package fr.laas.fape.ros.action;

import fr.laas.fape.ros.ROSUtils;
import fr.laas.fape.ros.exception.ActionFailure;
import pr2motion.*;

public class MoveTorsoActionServer {

    private static SimpleActionServer<Torso_MoveActionGoal, Torso_MoveActionFeedback, Torso_MoveActionResult> instance = null;

    public static SimpleActionServer<Torso_MoveActionGoal, Torso_MoveActionFeedback, Torso_MoveActionResult> getInstance() {
        if(instance == null) {
            instance = new SimpleActionServer<>("pr2motion/Torso_Move", Torso_MoveActionGoal._TYPE, Torso_MoveActionFeedback._TYPE, Torso_MoveActionResult._TYPE);
        }
        return instance;
    }

    public static Torso_MoveActionResult moveTorso(double position) throws ActionFailure {
        try {
            if (position < 0.0115 || position > 0.325)
                throw new IllegalArgumentException("Position of torso is invalid");
            Torso_MoveGoal goal = ROSUtils.emptyMessageFromType("pr2motion/Torso_MoveGoal");
            goal.setTorsoPosition((float) position);
            Torso_MoveActionResult res = getInstance().sendGoal(goal);
            if (res.getResult().getGenomSuccess())
                return res;
            else
                throw new ActionFailure("MoveTorso: " + res.getResult().getGenomExdetail());
        } catch (ActionFailure e) {
            throw e;
        } catch (Throwable e) {
            throw new ActionFailure("MoveTorso: unexpected exception.");
        }
    }
}
