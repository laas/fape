package fr.laas.fape.planning.core.planning.search.flaws.flaws;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.BindingSeparation;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.TemporalSeparation;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;

import java.util.LinkedList;
import java.util.List;

public class Threat extends Flaw {

    public final Timeline db1;
    public final Timeline db2;

    public Threat(Timeline db1, Timeline db2) {
        this.db1 = db1;
        this.db2 = db2;
    }

    @Override
    public List<Resolver> getResolvers(State st, Planner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();

        // db1 before db2
        if(st.canBeBefore(db1.getLastTimePoints().getFirst(), db2.getFirstTimePoints().getFirst()))
            resolvers.add(new TemporalSeparation(db1, db2));

        // db2 before db1
        if(st.canBeBefore(db2.getLastTimePoints().getFirst(), db1.getFirstTimePoints().getFirst()))
            resolvers.add(new TemporalSeparation(db2, db1));

        // make any argument of the state variables different
        for (int i = 0; i < db1.stateVariable.args().length; i++) {
            if(st.separable(db1.stateVariable.arg(i), db2.stateVariable.arg(i)))
                resolvers.add(new BindingSeparation(
                        db1.stateVariable.arg(i),
                        db2.stateVariable.arg(i)));
        }

        return resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof Threat;
        Threat t = (Threat) o;
        if(t.db1.mID != db1.mID)
            return t.db1.mID - db1.mID;
        else {
            assert t.db2.mID != db2.mID;
            return t.db2.mID - db2.mID;
        }
    }
}
