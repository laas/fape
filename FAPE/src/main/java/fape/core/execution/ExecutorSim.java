package fape.core.execution;

import fape.core.execution.model.AtomicAction;
import planstack.anml.model.concrete.ActRef;

import java.util.Timer;
import java.util.TimerTask;

public class ExecutorSim extends Executor {

    class SuccessTimerTask extends TimerTask {
        ActRef id;
        int end;
        public SuccessTimerTask(ActRef id, int endTime) {
            this.id = id;
            this.end = endTime;
        }
        @Override
        public void run() {
            mActor.ReportSuccess(id, end);
        }
    }

    class FailureTimerTask extends TimerTask {
        ActRef id;
        int end;
        public FailureTimerTask(ActRef id, int endTime) {
            this.id = id;
            this.end = endTime;
        }
        @Override
        public void run() {
            mActor.ReportFailure(id, end);
        }
    }

    @Override
    public void executeAtomicActions(AtomicAction acts) {
        System.out.println("Executing: "+acts+ " start: "+acts.mStartTime+", dur: ["+acts.minDuration+","+acts.maxDuration+"]");

        boolean failure = Math.random() * 100 > 80.0;
        int randomDur = (int) (acts.minDuration + (acts.maxDuration-acts.minDuration) * Math.random());
        Timer timer = new Timer();
        if(failure) {
            timer.schedule(new FailureTimerTask(acts.id, (int) (acts.mStartTime + randomDur)), randomDur * 1000);
        } else {
            timer.schedule(new SuccessTimerTask(acts.id, (int) (acts.mStartTime + randomDur)), randomDur * 1000);
        }
    }
}
