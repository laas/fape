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
package fape.acting;

import fape.execution.Executor;
import fape.model.ANMLPayload;
import fape.model.AtomicAction;
import fape.planning.Planner;
import fape.util.Pair;
import fape.util.TimeAmount;
import fape.util.TimePoint;
import java.util.List;

/**
 *
 * @author FD
 */
public class Actor {

    public void bind(Executor e, Planner p){
        mExecutor = e;
        mPlanner = p;
    }
    
    long sleepTime = 100;
    long repairTime = 300;
    long progressTime = 100;
    long progressStep = 100;
    Executor mExecutor;
    Planner mPlanner;
    public List<ANMLPayload> newEventBuffer;

    public enum EActorState {

        STOPPED, ACTING, ENDING
    }
    EActorState mState = EActorState.STOPPED;

    /**
     * runs the acting agent
     */
    public void run() throws InterruptedException {
        boolean end = false;
        while (!end) {
            switch (mState) {
                case ENDING:
                    end = true;
                case STOPPED:
                    Thread.sleep(sleepTime);
                    break;
                case ACTING:
                    while (!newEventBuffer.isEmpty()) {
                        mPlanner.ForceFact(null);
                    }
                    //performing repair and progress here
                    mPlanner.Repair(new TimeAmount(repairTime));
                    List<Pair<AtomicAction, TimePoint>> scheduledActions = mPlanner.Progress(new TimeAmount(progressStep), new TimeAmount(repairTime));
                    mExecutor.executeAtomicActions(scheduledActions);
                    break;
            }
        }
    }
}
