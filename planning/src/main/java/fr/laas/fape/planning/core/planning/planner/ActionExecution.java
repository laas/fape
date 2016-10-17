package fr.laas.fape.planning.core.planning.planner;

import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.ActRef;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.ActionStatus;

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