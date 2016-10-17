package fr.laas.fape.planning.core.planning.search.strategies.flaws;

import fr.laas.fape.anml.model.concrete.Task;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnrefinedTask;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.WeakHashMap;

public class HierFIFO implements FlawComparator {

    public final State st;
    public final Planner planner;

    public HierFIFO(State st, Planner planner) {
        this.st = st;
        this.planner = planner;
    }

    public static WeakHashMap<Task, Integer> encounterDepth = new WeakHashMap<>();

    @Override
    public String shortName() {
        return "hier-fifo";
    }

    private int priority(Flaw flaw) {
        if(!(flaw instanceof UnrefinedTask))
            return 99999;

        UnrefinedTask ut = (UnrefinedTask) flaw;
        final int depth = st.getDepth();
        return encounterDepth.computeIfAbsent(ut.task, task -> depth);
    }


    @Override
    public int compare(Flaw o1, Flaw o2) {
        return priority(o1) - priority(o2);
    }
}
