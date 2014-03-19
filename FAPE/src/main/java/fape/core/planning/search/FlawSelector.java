package fape.core.planning.search;


import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.Pair;

import java.util.Comparator;
import java.util.List;

//public class FlawSelector implements Comparator<Pair<Flaw, List<SupportOption>>> {
//
//    private final State state;
//
//    public FlawSelector(State st) {
//        this.state = st;
//    }
//
//    private int priority(Pair<Flaw, List<SupportOption>> flawAndResolvers) {
//        Flaw flaw = flawAndResolvers.value1;
//        List<SupportOption> options = flawAndResolvers.value2;
//        int base;
//        if(options.size() <= 1) {
//            base = 0;
//        } else if(flaw instanceof UnsupportedDatabase) {
//            TemporalDatabase consumer = ((UnsupportedDatabase) flaw).consumer;
//            String predicate = consumer.stateVariable.predicateName;
//            String argType = state.GetType(consumer.stateVariable.variable.GetReference());
//            String valueType = state.GetType(consumer.GetGlobalConsumeValue().GetReference());
//            int level = state.pb.hierarchy.getLevel(predicate, argType, valueType);
//            base = (level +1) * 500;
//        } else {
//            base = 99999;
//        }
//
//        return base + options.size();
//    }
//
//
//    @Override
//    public int compare(Pair<Flaw, List<SupportOption>> o1, Pair<Flaw, List<SupportOption>> o2) {
//        return priority(o1) - priority(o2);
//    }
//};