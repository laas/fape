package fr.laas.fape.planning.core.planning.search.flaws.finders;

import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.UnboundVariable;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;

public class UnboundVariableFinder implements FlawFinder {
    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        List<Flaw> flaws = new LinkedList<>();
        for (VarRef v : st.getUnboundVariables()) {
            assert !v.getType().isNumeric();
            flaws.add(new UnboundVariable(v));
        }
        return flaws;
    }
}
