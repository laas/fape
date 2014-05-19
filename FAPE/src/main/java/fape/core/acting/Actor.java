/*
* Author:  Filip Dvořák <filip.dvorak@runbox.com>
*
* Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
*
* Publishing, providing further or using this program is prohibited
* without previous written permission of the author. Publishing or providing
* further the contents of this file is prohibited without previous written
* permission of the author.
*/
package fape.core.acting;

import fape.core.execution.Executor;
import fape.core.execution.model.AtomicAction;
import fape.core.planning.Planner;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.parser.ParseResult;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import java.util.LinkedList;
import java.util.List;

/**
*
* @author FD
*/
public class Actor {

    public String pbName = "default";

    /**
     *
     * @param b
     */
    public void PushEvent(ParseResult b) {
        newEventBuffer.add(b);
    }

    /**
     *
     * @param e
     * @param p
     */
    public void bind(Executor e, Planner p) {
        mExecutor = e;
        mPlanner = p;
    }
    long sleepTime = 100;
    long repairTime = 10000;
    long progressTime = 10000;
    long progressStep = 5000;
    int currentDelay = 0;
    Executor mExecutor;
    Planner mPlanner;

    boolean planNeedsRepair = false;

    List<AtomicAction> actionsToDispatch = new LinkedList<>();
    LinkedList<ActRef> failures = new LinkedList<>();
    boolean planReady = false;

    /**
     *
     */
    public LinkedList<ParseResult> newEventBuffer = new LinkedList<>();
    public HashMap<ActRef, String> idToSignature = new HashMap<>();
    HashSet<String> successfulActions = new HashSet<>();
    HashSet<ActRef> dispatchedActions = new HashSet<>();
    List<AtomicAction> actionsBeingExecuted = new LinkedList<>();

    public void ReportSuccess(ActRef actionID, int realEndTime) {
        planNeedsRepair = true;
        successfulActions.add(idToSignature.get(actionID));
        AtomicAction act = null;
        for (AtomicAction a : actionsBeingExecuted) {
            if (a.id == actionID) {
                act = a;
            }
        }
        actionsBeingExecuted.remove(act);
        mPlanner.AddActionEnding(actionID, realEndTime);
    }

    public void ReportFailure(ActRef actionID) {
        planNeedsRepair = true;
        failures.add(actionID);
        AtomicAction act = null;
        for (AtomicAction a : actionsBeingExecuted) {
            if (a.id == actionID) {
                act = a;
            }
        }
        actionsBeingExecuted.remove(act);
    }

    /**
     *
     */
    public enum EActorState {

        /**
         *
         */
        STOPPED,
        /**
         *
         */
        ACTING,
        /**
         *
         */
        ENDING
    }
    EActorState mState = EActorState.ACTING;

    /**
     * runs the acting agent
     *
     * @throws java.lang.InterruptedException
     */
    public void run() throws InterruptedException {
        int timeZero = (int) (System.currentTimeMillis() / 1000);

        /*int successReportOne = timeZero + 5;
        int firstFailureTime = timeZero + 6;
        int newStatementTime = timeZero + 7;*/
        String report = "";

        boolean finished = false;
        boolean isInitPlan = true;
        int prevOpen = 0;
        int prevGene = 0;

        while (!finished) {
            switch (mState) {
                case ACTING:
                    int now = (int) (System.currentTimeMillis() / 1000);
                    if (!failures.isEmpty()) {
                        while (!failures.isEmpty()) {
                            mPlanner.FailAction(failures.pop());
                        }
                        planNeedsRepair = true;
                    }
                    if (!newEventBuffer.isEmpty()) {
                        while (!newEventBuffer.isEmpty()) {
                            mPlanner.ForceFact(newEventBuffer.pop());
                        }
                        planNeedsRepair = true;
                    }
                    if (planNeedsRepair) {
                        planNeedsRepair = false;
                        mPlanner.SetEarliestExecution((int) (now - timeZero + 1));
                        mPlanner.Repair(new TimeAmount(repairTime));
                        if(mPlanner.planState == Planner.EPlanState.CONSISTENT) {
                            now = (int) (System.currentTimeMillis() / 1000);
                            mPlanner.SetEarliestExecution((int) (now - timeZero));
                            mPlanner.Repair(new TimeAmount(500));
                        }

                        if(isInitPlan && mPlanner.planState == Planner.EPlanState.CONSISTENT) {
                            isInitPlan = false;
                            report += pbName + ", PLAN, " + (mPlanner.OpenedStates - prevOpen) + ", " + (mPlanner.GeneratedStates - prevGene) + "\n";
                        } else {
                            report += pbName+", REPAIR, "+(mPlanner.OpenedStates-prevOpen)+", "+(mPlanner.GeneratedStates - prevGene) + "\n";
                        }
                        prevGene = mPlanner.GeneratedStates;
                        prevOpen = mPlanner.OpenedStates;
                    }
                    if(mPlanner.planState == Planner.EPlanState.CONSISTENT && mPlanner.hasPendingActions()) {
                        List<AtomicAction> scheduledActions = mPlanner.Progress(new TimeAmount(now - timeZero + progressStep/1000));
                        actionsToDispatch.addAll(scheduledActions);
                    }
                    List<AtomicAction> remove = new LinkedList<>();
                    for (AtomicAction a : actionsToDispatch) {
                        if (a.mStartTime + timeZero < now && !successfulActions.contains(idToSignature.get(a.id)) && !dispatchedActions.contains(a.id)) {
                            idToSignature.put(a.id, a.GetDescription());
                            mExecutor.executeAtomicActions(a);
                            dispatchedActions.add(a.id);
                            actionsBeingExecuted.add(a);
                            remove.add(a);
                        }
                    }
                    actionsToDispatch.removeAll(remove);
                    remove.clear();
                    actionsBeingExecuted.removeAll(remove);

                    if(mPlanner.planState != Planner.EPlanState.UNINITIALIZED) {
                        if(mPlanner.planState == Planner.EPlanState.CONSISTENT && mPlanner.numUnfinishedActions() == 0) {
                            mState = EActorState.ENDING;
                            break;
                        }
                        if(mPlanner.planState == Planner.EPlanState.INCONSISTENT || mPlanner.planState == Planner.EPlanState.INFESSIBLE) {
                            mState = EActorState.STOPPED;
                            break;
                        }
                    }

                    Thread.sleep(sleepTime);
                    break;

                case ENDING:
                    System.err.println("Plan successfully carried out");
                    finished = true;
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("repair-states-count.csv", true)));
                        out.println(report);
                        out.close();
                    } catch (Exception e) {
                        throw new FAPEException("Cannot open file");
                    }
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("failures.csv", true)));
                        out.println(pbName + ", OK");
                        out.close();
                    } catch (Exception e) {
                        throw new FAPEException("Cannot open file");
                    }
                    System.exit(0);
                    break;
                case STOPPED:
                    System.err.println("Problem while executing the plan.");
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("failures.csv", true)));
                        if(isInitPlan) {
                            out.println(pbName + ", NOPLAN");
                        } else {
                            out.println(pbName + ", NOREPAIR");
                        }
                        out.close();
                    } catch (Exception e) {
                        throw new FAPEException("Cannot open file");
                    }
                    finished = true;
                    System.exit(0);
                    break;

            }
        }
    }
}
