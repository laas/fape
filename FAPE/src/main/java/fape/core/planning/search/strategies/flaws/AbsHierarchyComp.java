package fape.core.planning.search.strategies.flaws;


import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.AbstractionHierarchy;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnsupportedDatabase;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.Pair;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.VarRef;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A comparator for flaws and their resolvers that uses lifted abstraction hierarchies.
 *
 * The ordering places unsupported databases first. Other flaws are left unordered
 * Unsupported databases are ordered according to their level in the abstraction hierarchy.
 *
 */
public class AbsHierarchyComp implements FlawComparator {

    private final State state;
    private final APlanner planner;
    private AbstractionHierarchy hierarchy;

    /**
     * Map AnmlProblems to a pair (n, h) where h is the abstraction hierarchy for the nth revision
     * of the problem.
     */
    static Map<AnmlProblem, Pair<Integer, AbstractionHierarchy>> hierarchies = new HashMap<>();

    public AbsHierarchyComp(State st, APlanner planner) {
        this.state = st;
        this.planner = planner;
        if(!hierarchies.containsKey(st.pb) || hierarchies.get(st.pb).value1 != st.pb.modifiers().size()) {
            hierarchies.put(st.pb, new Pair<>(st.pb.modifiers().size(), new AbstractionHierarchy(st.pb)));
        }
        this.hierarchy = hierarchies.get(st.pb).value2;
    }

    private int priority(Flaw flaw) {
        final int numResolvers = flaw.getNumResolvers(state, planner);
        int level;

        if(numResolvers == 0) {
            // Dead end end, make sure it comes out first.
            return -2;
        } else if(numResolvers == 1) {
            // Only one option, make sure it comes right after dead-ends.
            return -1;
        } else if(flaw instanceof UnsupportedDatabase) {
            // open link, order them according to their level in the abstraction hierarchy
            TemporalDatabase consumer = ((UnsupportedDatabase) flaw).consumer;
            String predicate = consumer.stateVariable.func().name();
            List<String> argTypes = new LinkedList<>();
            for(VarRef argVar : consumer.stateVariable.jArgs()) {
                argTypes.add(state.typeOf(argVar));
            }
            String valueType = state.typeOf(consumer.GetGlobalConsumeValue());
            level = hierarchy.getLevel(predicate, argTypes, valueType);
        } else {
            // a flaw (which is not an open link) with at least 2 resolvers, set priority to lowest.
            level = Integer.MAX_VALUE;
        }

        return level;
    }


    @Override
    public int compare(Flaw o1, Flaw o2) {
        return priority(o1) - priority(o2);
    }

    @Override
    public String shortName() {
        return "abs";
    }
};