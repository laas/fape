package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.Planner;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import lombok.Value;
import planstack.anml.model.concrete.Task;

@Value
public class FutureTaskSupport implements Resolver {

    private final Timeline consumer;
    private final Task task;

    @Override
    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        st.getHierarchicalConstraints().setSupportConstraint(consumer, task);
        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof FutureTaskSupport;
        FutureTaskSupport fts = (FutureTaskSupport) e;
        assert fts.consumer == consumer;

        return Integer.compare(task.start().id(), fts.task.start().id());
    }
}
