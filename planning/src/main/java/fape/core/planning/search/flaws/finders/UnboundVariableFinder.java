package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.UnboundVariable;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.VarRef;

import java.util.LinkedList;
import java.util.List;

public class UnboundVariableFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, APlanner planner) {
        List<Flaw> flaws = new LinkedList<>();
        for (VarRef v : st.getUnboundVariables()) {
            assert !v.getType().isNumeric();
            flaws.add(new UnboundVariable(v));
        }
        return flaws;
    }
}
