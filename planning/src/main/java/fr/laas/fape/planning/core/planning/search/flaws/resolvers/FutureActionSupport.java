package fr.laas.fape.planning.core.planning.search.flaws.resolvers;

import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import lombok.Value;
import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;

@Value
public class FutureActionSupport implements Resolver {

    private final Timeline consumer;
    private final AbstractAction act;

    @Override
    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        assert consumer != null : "Consumer was not found.";

        Action action = Factory.getStandaloneAction(st.pb, act, st.refCounter);
        st.insert(action);
        st.getHierarchicalConstraints().setSupportConstraint(consumer, action);
        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof FutureActionSupport;
        FutureActionSupport fts = (FutureActionSupport) e;
        assert fts.consumer == consumer : "Comparing resolvers of different flaws.";
        return act.name().compareTo(fts.act.name());
    }
}
