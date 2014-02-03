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
import fape.core.execution.model.ANMLBlock;
import fape.core.execution.model.AtomicAction;
import fape.core.planning.Planner;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TimePoint;
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

    /**
     *
     * @param b
     */
    public void PushEvent(ANMLBlock b) {
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
    long repairTime = 5000;
    long progressTime = 10000;
    long progressStep = 5000;
    int currentDelay = 0;
    Executor mExecutor;
    Planner mPlanner;

    boolean planNeedsRepair = false;

    List<AtomicAction> actionsToDispatch = new LinkedList<>();
    LinkedList<Integer> failures = new LinkedList<>();
    boolean planReady = false;

    /**
     *
     */
    public LinkedList<ANMLBlock> newEventBuffer = new LinkedList<>();
    public HashMap<Integer, String> idToSignature = new HashMap<>();
    HashSet<String> successfulActions = new HashSet<>();
    HashSet<Integer> dispatchedActions = new HashSet<>();
    List<AtomicAction> actionsBeingExecuted = new LinkedList<>();

    public void ReportSuccess(int actionID, int realEndTime) {
        planNeedsRepair = true;
        successfulActions.add(idToSignature.get(actionID));
        AtomicAction act = null;
        for (AtomicAction a : actionsBeingExecuted) {
            if (a.mID == actionID) {
                act = a;
            }
        }
        actionsBeingExecuted.remove(act);
        mPlanner.AddActionEnding(actionID, realEndTime - currentDelay);
    }

    public void ReportFailure(int actionID) {
        planNeedsRepair = true;
        failures.add(actionID);
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

        while (true) {
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
                        mPlanner.Repair(new TimeAmount(repairTime));
                        List<AtomicAction> scheduledActions = mPlanner.Progress(new TimeAmount(now - timeZero - currentDelay + progressStep), new TimeAmount(repairTime));
                        actionsToDispatch = new LinkedList<>(scheduledActions);
                    }
                    List<AtomicAction> remove = new LinkedList<>();
                    for (AtomicAction a : actionsToDispatch) {
                        if (a.mStartTime + timeZero + currentDelay < now && !successfulActions.contains(idToSignature.get(a.mID)) && !dispatchedActions.contains(a.mID)) {
                            idToSignature.put(a.mID, a.GetDescription());
                            if ((now - timeZero) - a.mStartTime > currentDelay) {
                                currentDelay = (now - timeZero) - a.mStartTime;
                            }
                            a.mStartTime = (int) (currentDelay + a.mStartTime);
                            mExecutor.executeAtomicActions(a);
                            dispatchedActions.add(a.mID);
                            actionsBeingExecuted.add(a);
                            remove.add(a);
                        }
                    }
                    actionsToDispatch.removeAll(remove);
                    remove.clear();
                    for (AtomicAction a : actionsBeingExecuted) {
                        if (a.duration + a.mStartTime + timeZero < now) {
                            ReportFailure(a.mID);
                            remove.add(a);
                        }
                    }
                    actionsBeingExecuted.removeAll(remove);
                    Thread.sleep(sleepTime);
                    break;
            }
        }
    }
}
