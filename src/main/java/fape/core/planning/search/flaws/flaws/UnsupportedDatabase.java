package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.*;
import fape.core.planning.search.flaws.resolvers.*;
import fape.core.planning.search.flaws.resolvers.SupportingAction;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.*;

public class UnsupportedDatabase extends Flaw {

    public final TemporalDatabase consumer;

    public UnsupportedDatabase(TemporalDatabase tdb) {
        this.consumer = tdb;
    }

    @Override
    public String toString() {
        return "Unsupported: " + consumer;
    }

    /**
     * Returns true if the first time-point *must* occur *exactly* one time unit before the second one.
     */
    private boolean areNecessarilyGlued(State st, TPRef first, TPRef second) {
        return !st.csp.stn().isConstraintPossible(first, second, 0)
                && !st.csp.stn().isConstraintPossible(second, first, -2);
    }

    /**
     * Returns true if the two state variables are necessarily identical.
     * This is true if they are on the same state variable and all their arguments are equals.
     */
    private boolean areNecessarilyIdentical(State st, ParameterizedStateVariable sv1, ParameterizedStateVariable sv2) {
        if(sv1.func() != sv2.func())
            return false;
        for(int i=0 ; i<sv1.args().size() ; i++) {
            if(st.separable(sv1.jArgs().get(i), sv2.jArgs().get(i)))
                return false;
        }
        return true;
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
            if (consumer == b || !st.unifiable(consumer, b)) {
                continue;
            }
            // if the database has a single persistence we try to integrate it with other persistences.
            // except if the state variable is constant, in which case looking only for the assignments saves search effort.
            if (consumer.hasSinglePersistence() && !consumer.stateVariable.func().isConstant()) {
                //we are looking for chain integration too
                int ct = 0;
                for (ChainComponent comp : b.chain) {
                    if (comp.change && st.unifiable(comp.getSupportValue(), consumer.getGlobalConsumeValue())
                            && st.canBeBefore(comp.getSupportTimePoint(), consumer.getConsumeTimePoint())) {
                        resolvers.add(new SupportingDatabase(b.mID, ct, consumer));
                    }
                    ct++;
                }

                // Otherwise, check for databases containing a change whose support value can
                // be unified with our consume value.
            } else if (st.unifiable(b.getGlobalSupportValue(), consumer.getGlobalConsumeValue())
                    && !b.hasSinglePersistence()
                    && st.canBeBefore(b.getSupportTimePoint(), consumer.getConsumeTimePoint())) {
                resolvers.add(new SupportingDatabase(b.mID, consumer));
            }
        }

        // checks all the given resolvers to check if one must be applied.
        // this is true iff the supporter is non separable from the consumer:
        //    they are necessarily on the same state variable and they cannot be temporally separated.
        // If such a resolver if found, it means that no other resolver is applicable and only this one is returned.
        for(Resolver res : resolvers) {
            SupportingDatabase sdb = (SupportingDatabase) res;
            ChainComponent supportingCC = null;
            if(sdb.precedingChainComponent == -1) {
                supportingCC = st.getDatabase(sdb.supporterID).chain.getLast();
            } else {
                supportingCC = st.getDatabase(sdb.supporterID).getChainComponent(sdb.precedingChainComponent);
            }
            ChainComponent consumingCC = st.getDatabase(sdb.consumerID).getChainComponent(0);

            for(LogStatement sup : supportingCC.contents) {
                for(LogStatement cons : consumingCC.contents) {
                    if(areNecessarilyGlued(st, sup.end(), cons.start()) && areNecessarilyIdentical(st, sup.sv(), cons.sv())) {
                        resolvers.clear();
                        resolvers.add(res);
                        return resolvers;
                    }
                }
            }
        }

        // here we check to see if some resolvers are not valid because resulting in unsolvable threats
        List<Resolver> toRemove = new LinkedList<>();
        for(Resolver res : resolvers) {
            TemporalDatabase supporter = st.getDatabase(((SupportingDatabase) res).supporterID);
            for(TemporalDatabase other : st.getDatabases()) {
                if(other != consumer && other != supporter)
                if(!other.hasSinglePersistence() && (st.unified(other, supporter) || st.unified(other, consumer))) {
                    // other is on the same state variable and chages the value of the state variable
                    // if it must be between the two, it will result in an unsolvable threat
                    // hence it must can be after the end of the consumer or before the start of the
                    if(!st.canBeBefore(consumer.getLastTimePoints(), other.getFirstChangeTimePoint())
                            && !st.canBeBefore(other.getSupportTimePoint(), supporter.getFirstTimePoints())) {
                        // will result in unsolvable threat
                        toRemove.add(res);
                    }
                }
            }
        }
        resolvers.removeAll(toRemove);

        // adding actions
        // ... the idea is to decompose actions as long as they provide some support that I need, if they cant, I start adding actions
        //find actions that help me with achieving my value through some decomposition in the task netAbstractActionwork
        //they are those that I can find in the virtual decomposition tree
        //first get the action names from the abstract dtgs
        ActionSupporterFinder supporters = planner.getActionSupporterFinder();
        ActionDecompositions decompositions = new ActionDecompositions(st.pb);

        // a list of (abstract-action, decompositionID) of supporters
        Collection<fape.core.planning.preprocessing.SupportingAction> potentialSupporters = supporters.getActionsSupporting(st, consumer);

        // all actions that have an effect on the state variable
        Set<AbstractAction> potentiallySupportingAction = new HashSet<>();
        for(fape.core.planning.preprocessing.SupportingAction sa : potentialSupporters)
            potentiallySupportingAction.add(sa.absAct);


        for (Action leaf : st.getOpenLeaves()) {
            for (Integer decID : decompositions.possibleDecompositions(leaf, potentiallySupportingAction)) {
                resolvers.add(new SupportingActionDecomposition(leaf, decID, consumer));
            }
        }

        //now we can look for adding the actions ad-hoc ...
        if (APlanner.actionResolvers) {
            for (fape.core.planning.preprocessing.SupportingAction aa : potentialSupporters) {
                // only considere action that are not marked motivated.
                // TODO: make it complete (consider a task hierarchy where an action is a descendant of unmotivated action)
                if (planner.useActionConditions() || !aa.absAct.mustBeMotivated()) {
                    if(st.isAddable(aa.absAct))
                        resolvers.add(new SupportingAction(aa.absAct, aa.decID, aa.statementRef, consumer));
                }
            }
        }

        return this.resolvers;
    }
}
