package fape.core.planning.planner;


import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionStatus;

import java.util.List;

public class ActionExecution {
    final ActRef id;
    final AbstractAction abs;
    long startTime;
    long endTime;
    List<String> args;
    ActionStatus status;


    public ActionExecution(Action a, List<String> args, long startTime) {
        this.id = a.id();
        this.abs = a.abs();
        this.startTime = startTime;
        this.args = args;
        this.status = ActionStatus.EXECUTING;
    }
}