package fr.laas.fape.planning.core.planning.search.strategies.flaws;



import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.planning.core.planning.preprocessing.AbstractionHierarchy;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnsupportedTimeline;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * A comparator for flaws and their resolvers that uses lifted abstraction hierarchies.
 *
 * The ordering places unsupported databases first. Other flaws are left unordered
 * Unsupported databases are ordered according to their level in the abstraction hierarchy.
 *
 */
public class AbsHierarchyComp implements FlawComparator {

    private AbstractionHierarchy hierarchy;

    /**
     * Map AnmlProblems to a pair (n, h) where h is the abstraction hierarchy for the nth revision
     * of the problem.
     */
    static Map<AnmlProblem, Pair<Integer, AbstractionHierarchy>> hierarchies = new HashMap<>();

    public AbsHierarchyComp(State st) {
        if(!hierarchies.containsKey(st.pb) || hierarchies.get(st.pb).value1 != st.pb.chronicles().size()) {
            hierarchies.put(st.pb, new Pair<>(st.pb.chronicles().size(), new AbstractionHierarchy(st.pb)));
        }
        this.hierarchy = hierarchies.get(st.pb).value2;
    }

    private int priority(UnsupportedTimeline og) {
        // open goal, order them according to their level in the abstraction hierarchy
        return hierarchy.getLevel(og.consumer.stateVariable.func());
    }


    @Override
    public int compare(Flaw o1, Flaw o2) {
        if(o1 instanceof UnsupportedTimeline && o2 instanceof UnsupportedTimeline)
            return priority((UnsupportedTimeline) o1) - priority((UnsupportedTimeline) o2);
        else
            return 0;
    }

    @Override
    public String shortName() {
        return "abs";
    }
}