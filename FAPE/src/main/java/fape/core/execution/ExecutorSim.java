package fape.core.execution;

import fape.core.execution.model.AtomicAction;

public class ExecutorSim extends Executor {

    @Override
    public void executeAtomicActions(AtomicAction acts) {
        System.out.println("Executing: "+acts+ " start: "+acts.mStartTime+", dur: ["+acts.minDuration+","+acts.maxDuration+"]");
        int randomNum = (int)(Math.random()*100);

        if(randomNum < 90) {
            mActor.ReportSuccess(acts.id, (int) acts.mStartTime + acts.maxDuration);
        } else {
            mActor.ReportFailure(acts.id);
        }
    }
}
