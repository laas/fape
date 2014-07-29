package fape.core.execution;

import com.sun.net.httpserver.Authenticator;import fape.core.execution.model.AtomicAction;

public class ExecutorSim extends Executor {

    @Override
    public void executeAtomicActions(AtomicAction acts) {
        System.out.println("Executing: "+acts+ " start: "+acts.mStartTime+", dur:"+acts.duration);
        int randomNum = (int)(Math.random()*100);

        if(randomNum > 10) {
            mActor.ReportSuccess(acts.id, (int) acts.mStartTime + acts.duration);
        } else {
            System.out.println("FAIL");
            mActor.ReportFailure(acts.id);
        }
    }
}
