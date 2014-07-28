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

import fape.FAPE;
import fape.core.execution.Executor;
import fape.core.execution.model.AtomicAction;
import fape.core.planning.Planner;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import fape.util.TimeAmount;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.parser.ParseResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
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
    public synchronized void PushEvent(ParseResult b) {
        newEventBuffer.add(b);
    }

    /**
     *
     * @param e
     * @param p
     */
    public synchronized void bind(Executor e, Planner p) {
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
    LinkedList<Pair<ActRef, Integer>> successes = new LinkedList<>();
    boolean planReady = false;

    boolean finished = false;
    boolean isInitPlan = true;
    int prevOpen = 0;
    int prevGene = 0;
    String report = "";
    int timeZero;

    /**
     *
     */
    public LinkedList<ParseResult> newEventBuffer = new LinkedList<>();
    public HashMap<ActRef, String> idToSignature = new HashMap<>();
    HashSet<String> successfulActions = new HashSet<>();
    HashSet<ActRef> dispatchedActions = new HashSet<>();
    List<AtomicAction> actionsBeingExecuted = new LinkedList<>();

    public synchronized void ReportSuccess(ActRef actionID, int realEndTime) {
        planNeedsRepair = true;
        successfulActions.add(idToSignature.get(actionID));
        AtomicAction act = null;
        for (AtomicAction a : actionsBeingExecuted) {
            if (a.id == actionID) {
                act = a;
            }
        }
        actionsBeingExecuted.remove(act);
        successes.add(new Pair<>(actionID, realEndTime));

    }

    public synchronized void ReportFailure(ActRef actionID) {
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

    public void run() throws InterruptedException {
        timeZero = (int) (System.currentTimeMillis() / 1000);

        while(!finished) {
            runOnce();
            if(!finished)
                Thread.sleep(sleepTime);
        }
    }

    int previousRun = -1;

    /**
     * runs the acting agent
     *
     * @throws java.lang.InterruptedException
     */
    private synchronized void runOnce() throws InterruptedException {
        int now = (int) (System.currentTimeMillis() / 1000);
        switch (mState) {
            case ACTING:
                while(!successes.isEmpty()) {
                    Pair<ActRef, Integer> actAndEndTime = successes.remove();
                    mPlanner.AddActionEnding(actAndEndTime.value1, actAndEndTime.value2);
                }
                if (!failures.isEmpty()) {
                    if(FAPE.execLogging) System.out.println("Including "+failures.size()+" failures");
                    while (!failures.isEmpty()) {
                        mPlanner.FailAction(failures.pop());
                    }
                    planNeedsRepair = true;
                }
                if (!newEventBuffer.isEmpty()) {
                    if(FAPE.execLogging) System.out.println("Including "+newEventBuffer.size()+" new events");
                    while (!newEventBuffer.isEmpty()) {
                        mPlanner.ForceFact(newEventBuffer.pop(), true);
                    }
                    planNeedsRepair = true;
                }
                if(previousRun >= 0 && now > previousRun) {
                    mPlanner.SetEarliestExecution((int) (now - timeZero + 1));
                    planNeedsRepair = true;
                    previousRun = now;
                }
                if (planNeedsRepair) {
                    if(FAPE.execLogging) System.out.println("Repairing Plan");
                    planNeedsRepair = false;
                    mPlanner.Repair(new TimeAmount(repairTime));
                    if(mPlanner.planState == Planner.EPlanState.CONSISTENT) {
                        now = (int) (System.currentTimeMillis() / 1000);
                        mPlanner.SetEarliestExecution((int) (now - timeZero));
                        mPlanner.Repair(new TimeAmount(500));
                    }

                    if(isInitPlan && mPlanner.planState == Planner.EPlanState.CONSISTENT) {
                        if(FAPE.execLogging) System.out.println("Plan consistent");
                        isInitPlan = false;
                        report += pbName + ", PLAN, " + (mPlanner.OpenedStates - prevOpen) + ", " + (mPlanner.GeneratedStates - prevGene) + "\n";
                    } else if(mPlanner.planState == Planner.EPlanState.CONSISTENT) {
                        if(FAPE.execLogging) System.out.println("Repaired plan consistent");
                        report += pbName+", REPAIR, "+(mPlanner.OpenedStates-prevOpen)+", "+(mPlanner.GeneratedStates - prevGene) + "\n";
                    } else {
                        if(FAPE.execLogging) System.out.println("Repaired plan FAILURE");
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
                break;
        }
    }
}
