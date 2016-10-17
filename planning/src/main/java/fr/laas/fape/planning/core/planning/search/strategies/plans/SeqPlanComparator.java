package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to use a sequence of PartialPlanComparator as one.
 *
 * The basic algorithm for comparing two partial plans is to apply the comparators in sequence until it results in an ordering
 * between the two plans. If no comparator is found, the plans are left unordered.
 */
public class SeqPlanComparator extends PartialPlanComparator {

    private final List<PartialPlanComparator> comparators;

    public SeqPlanComparator(List<PartialPlanComparator> comparators) {

        this.comparators = new LinkedList<>(comparators);
    }

    @Override
    public String shortName() {
        String ret = "";
        for(PartialPlanComparator comp : comparators) {
            ret += comp.shortName() + ",";
        }
        return ret.substring(0, ret.length()-1);
    }

    @Override
    public String reportOnState(State st) {
        StringBuilder sb = new StringBuilder();
        for (PartialPlanComparator ppc : comparators) {
            sb.append(ppc.reportOnState(st));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public double g(State st) {
        double v = 0;
        for(PartialPlanComparator pc : comparators)
            v = 1000000 * v + pc.g(st);
        return v;
    }

    @Override
    public double h(State st) {
        double v = 0;
        for(PartialPlanComparator pc : comparators)
            v = 1000000 * v + pc.h(st);
        return v;
    }

    @Override
    public double hc(State st) {
        double v = 0;
        for(PartialPlanComparator pc : comparators)
            v = 1000000 * v + pc.hc(st);
        return v;
    }
}
