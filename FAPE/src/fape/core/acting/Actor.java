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
    long repairTime = 300;
    long progressTime = 100;
    long progressStep = 100;
    Executor mExecutor;
    Planner mPlanner;

    boolean planNeedsRepair = false;

    List<AtomicAction> actionsToDispatch = new LinkedList<>();
    LinkedList<Integer> failures = new LinkedList<>();
    boolean planReady = false;
    int currentDelay = 0;

    /**
     *
     */
    public LinkedList<ANMLBlock> newEventBuffer = new LinkedList<>();
    public HashMap<Integer, String> idToSignature = new HashMap<>();
    HashSet<String> successfulActions = new HashSet<>();

    public void ReportSuccess(int actionID, int realEndTime) {
        successfulActions.add(idToSignature.get(actionID));
        mPlanner.AddActionEnding(actionID, realEndTime);
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
        boolean end = false;
        int timeZero = (int) (System.currentTimeMillis() / 1000);
        while (!end) {
            switch (mState) {
                /*case ENDING:
                 end = true;
                 case STOPPED:
                 Thread.sleep(sleepTime);
                 //mState = EActorState.ACTING;
                 break;*/
                case ACTING:
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
                        List<AtomicAction> scheduledActions = mPlanner.Progress(new TimeAmount(progressStep), new TimeAmount(repairTime));
                        Collections.sort(scheduledActions, new Comparator<AtomicAction>() {
                            @Override
                            public int compare(AtomicAction o1, AtomicAction o2) {
                                return (int) (o1.mStartTime - o2.mStartTime);
                            }
                        });
                        actionsToDispatch = new LinkedList<>(scheduledActions);
                    }
                    int now = (int) (System.currentTimeMillis() / 1000);
                    List<AtomicAction> remove = new LinkedList<>();
                    for (AtomicAction a : actionsToDispatch) {
                        if (a.mStartTime + timeZero + currentDelay < now && !successfulActions.contains(idToSignature.get(a.mID))) {
                            idToSignature.put(a.mID, a.GetDescription());
                            if((now - timeZero) - a.mStartTime > currentDelay){
                                currentDelay = (now - timeZero) - a.mStartTime;
                            }
                            a.mStartTime = (int) (currentDelay + a.mStartTime);                            
                            mExecutor.executeAtomicActions(a);
                            remove.add(a);
                        }
                    }
                    actionsToDispatch.removeAll(remove);
                    Thread.sleep(sleepTime);
                    break;
            }
        }
    }
}
