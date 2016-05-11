package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.Planner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import lombok.Value;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.Task;

@Value
public class FutureActionSupport extends Resolver {

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
