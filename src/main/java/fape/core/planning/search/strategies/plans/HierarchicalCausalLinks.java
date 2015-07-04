package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.structures.Pair;

/**
 * Gives priority to plan where causal links are between action part of the same hierarchy.
 */
public class HierarchicalCausalLinks implements PartialPlanComparator {
    @Override
    public String shortName() {
        return "hcl";
    }

    @Override
    public String reportOnState(State st) {
        return "HCL: f: "+f(st)+"causal-link-weight: "+weightCausalLinks(st);
    }

    /**
     * Map a causal link to a weight.
     * The basic idea here is to penalize causal link between actions that are not part of the same hierarchy.
     */
    int weightOfCausalLink(LogStatement sup, LogStatement cons, State st) {
        if(sup.sv().func().name().equals("Robot.loc") || sup.sv().func().name().equals("Dock.empty")) {
            return 0;
        }

        // get the two actions containing each statement
        Action aSup = st.getActionContaining(sup);
        Action aCons = st.getActionContaining(cons);
        if(aSup == null || aCons == null) {
            // at least one statement is part of the problem, no penalization
            return 0;
        } else {
            //
            Pair<Action, Integer> commonAncestor = st.taskNet.leastCommonAncestor(aSup, aCons);
            if(commonAncestor == null)
                // the two actions have no common ancestor, penalize
                return 1;
            else {
                // the two actions have a common ancestor, no penalization (might even be a bonus)
                return 0;
            }
        }
    }

    /**
     * Evaluates total weight of all causal link in the state.
     */
    int weightCausalLinks(State st) {
        int w = 0;
        for(Timeline db : st.getTimelines()) {
            for(Pair<LogStatement, LogStatement> cl : db.allCausalLinks()) {
                w += weightOfCausalLink(cl.v1(), cl.v2(), st);
            }
        }
        return w;
    }

    public float f(State s) {
        return weightCausalLinks(s) + s.getNumRoots() *4+ s.tdb.getConsumers().size() + s.getNumOpenLeaves();
    }

    @Override
    public int compare(State state, State state2) {
        float f_state = f(state);
        float f_state2 = f(state2);

        // comparison (and not difference) is necessary since the input is a float.
        if(f_state > f_state2)
            return 1;
        else if(f_state2 > f_state)
            return -1;
        else
            return 0;
    }
}
