package fape.core.planning.search;

import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.ActionDecompositions;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.search.resolvers.Resolver;
import fape.core.planning.search.resolvers.SupportingAction;
import fape.core.planning.search.resolvers.SupportingDatabase;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class UnsupportedDatabase extends Flaw {

    public final TemporalDatabase consumer;

    public UnsupportedDatabase(TemporalDatabase tdb) {
        this.consumer = tdb;
    }

    @Override
    public String toString() {
        return "Unsupported: " + consumer;
    }

    @Override
    public List<Resolver> getResolvers(State st, APlanner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();

        //here we need to find several types of supporters
        //1) chain parts that provide the value we need
        //2) actions that provide the value we need and can be added
        //3) tasks that can decompose into an action we need

        //get chain connections
        for (TemporalDatabase b : st.getDatabases()) {
            if (consumer == b || !st.Unifiable(consumer, b)) {
                continue;
            }
            // if the database has a single persistence we try to integrate it with other persistences.
            // except if the state variable is constant, in which case looking only for the assignments saves search effort.
            if (consumer.HasSinglePersistence() && !consumer.stateVariable.func().isConstant()) {
                //we are looking for chain integration too
                int ct = 0;
                for (ChainComponent comp : b.chain) {
                    if (comp.change && st.unifiable(comp.GetSupportValue(), consumer.GetGlobalConsumeValue())
                            && st.canBeBefore(comp.getSupportTimePoint(), consumer.getConsumeTimePoint())) {
                        resolvers.add(new SupportingDatabase(b.mID, ct));
                    }
                    ct++;
                }

                // Otherwise, check for databases containing a change whose support value can
                // be unified with our consume value.
            } else if (st.unifiable(b.GetGlobalSupportValue(), consumer.GetGlobalConsumeValue())
                    && !b.HasSinglePersistence()
                    && st.canBeBefore(b.getSupportTimePoint(), consumer.getConsumeTimePoint())) {
                resolvers.add(new SupportingDatabase(b.mID));
            }
        }

        // adding actions
        // ... the idea is to decompose actions as long as they provide some support that I need, if they cant, I start adding actions
        //find actions that help me with achieving my value through some decomposition in the task network
        //they are those that I can find in the virtual decomposition tree
        //first get the action names from the abstract dtgs
        ActionSupporterFinder supporters = planner.getActionSupporterFinder();
        ActionDecompositions decompositions = new ActionDecompositions(st.pb);
        Collection<AbstractAction> potentialSupporters = supporters.getActionsSupporting(st, consumer);

        for (Action leaf : st.getOpenLeaves()) {
            for (Integer decID : decompositions.possibleDecompositions(leaf, potentialSupporters)) {
                resolvers.add(new fape.core.planning.search.resolvers.Decomposition(leaf, decID));
            }
        }

        //now we can look for adding the actions ad-hoc ...
        if (APlanner.actionResolvers) {
            for (AbstractAction aa : potentialSupporters) {
                // only considere action that are not marked motivated.
                // TODO: make it complete (consider a task hierarchy where an action is a descendant of unmotivated action)
                if (planner.useActionConditions() || !aa.mustBeMotivated()) {
                    resolvers.add(new SupportingAction(aa));
                }
            }
        }

        return this.resolvers;
    }
}
