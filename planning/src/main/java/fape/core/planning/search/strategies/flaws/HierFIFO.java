package fape.core.planning.search.strategies.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnmotivatedAction;
import fape.core.planning.search.flaws.flaws.UnsupportedTask;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.Task;

import java.util.WeakHashMap;

public class HierFIFO implements FlawComparator {

    public final State st;
    public final APlanner planner;

    public HierFIFO(State st, APlanner planner) {
        this.st = st;
        this.planner = planner;
    }

    public static WeakHashMap<Task, Integer> encounterDepth = new WeakHashMap<>();

    @Override
    public String shortName() {
        return "hier-fifo";
    }

    private int priority(Flaw flaw) {
        if(!(flaw instanceof UnsupportedTask))
            return 99999;

        UnsupportedTask ut = (UnsupportedTask) flaw;
        final int depth = st.getDepth();
        return encounterDepth.computeIfAbsent(ut.task, task -> depth);
    }


    @Override
    public int compare(Flaw o1, Flaw o2) {
        return priority(o1) - priority(o2);
    }
}
