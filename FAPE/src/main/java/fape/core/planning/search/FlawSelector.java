package fape.core.planning.search;


import fape.core.planning.preprocessing.AbstractionHierarchy;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.Pair;
import planstack.anml.model.concrete.VarRef;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * A comparator for flaws and their resolvers that uses lifted abstraction hierarchies.
 */
public class FlawSelector implements Comparator<Pair<Flaw, List<SupportOption>>> {

    private final State state;
    private AbstractionHierarchy hierarchy;

    public FlawSelector(AbstractionHierarchy hierarchy, State st) {
        this.state = st;
        this.hierarchy = hierarchy;
    }

    private int priority(Pair<Flaw, List<SupportOption>> flawAndResolvers) {
        Flaw flaw = flawAndResolvers.value1;
        List<SupportOption> options = flawAndResolvers.value2;
        int base;
        if(options.size() <= 1) {
            base = 0;
        } else if(flaw instanceof UnsupportedDatabase) {
            TemporalDatabase consumer = ((UnsupportedDatabase) flaw).consumer;
            String predicate = consumer.stateVariable.func().name();
            List<String> argTypes = new LinkedList<>();
            for(VarRef argVar : consumer.stateVariable.jArgs()) {
                argTypes.add(state.conNet.typeOf(argVar));
            }
            String valueType = state.conNet.typeOf(consumer.GetGlobalConsumeValue());
            int level = hierarchy.getLevel(predicate, argTypes, valueType);
            base = (level +1) * 500;
        } else {
            base = 99999;
        }

        return base + options.size();
    }


    @Override
    public int compare(Pair<Flaw, List<SupportOption>> o1, Pair<Flaw, List<SupportOption>> o2) {
        return priority(o1) - priority(o2);
    }
};