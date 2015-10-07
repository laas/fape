package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.TaskDecompositions;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.search.flaws.resolvers.SupportingAction;
import fape.core.planning.search.flaws.resolvers.SupportingTaskDecomposition;
import fape.core.planning.search.flaws.resolvers.SupportingTimeline;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.LVarRef;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.AbstractAssignment;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.abs.statements.AbstractStatement;
import planstack.anml.model.abs.statements.AbstractTransition;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.*;
import java.util.function.Predicate;

public class UnsupportedTimeline extends Flaw {

    public final Timeline consumer;

    public UnsupportedTimeline(Timeline tdb) {
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
        for(int i=0 ; i<sv1.args().length ; i++) {
            if(st.separable(sv1.arg(i), sv2.arg(i)))
                return false;
        }
        return true;
    }

    /** Returns true if the nth change in potentialSupporter (n = changeNumber) can support the consumer timeline */
    public static boolean isSupporting(Timeline potentialSupporter, int changeNumber, Timeline consumer, State st) {
        if(!st.unifiable(potentialSupporter, consumer))
            return false;

        // if the consumer contains changes, the only possible support is the last change of the supporter
        if(!consumer.hasSinglePersistence() && changeNumber != potentialSupporter.numChanges()-1)
            return false;

        final ChainComponent supportingCC = potentialSupporter.getChangeNumber(changeNumber);
        if(!st.unifiable(supportingCC.getSupportValue(), consumer.getGlobalConsumeValue()))
            return false;

        if(!st.canAllBeBefore(supportingCC.getSupportTimePoint(), consumer.getFirstTimePoints()))
            return false;

        // if the supporter is not the last change, check that we can fit the consuming db before the next change
        if(changeNumber < potentialSupporter.numChanges()-1) {
            final ChainComponent afterCC = potentialSupporter.getChangeNumber(changeNumber+1);
            if(!st.canAllBeBefore(consumer.getLastTimePoints(), afterCC.getConsumeTimePoint()))
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

        resolvers.addAll(st.getTimelineSupportersFor(consumer));

        // checks all the given resolvers to check if one must be applied.
        // this is true iff the supporter is non separable from the consumer:
        //    they are necessarily on the same state variable and they cannot be temporally separated.
        // If such a resolver if found, it means that no other resolver is applicable and only this one is returned.
        for(Resolver res : resolvers) {
            SupportingTimeline sdb = (SupportingTimeline) res;
            ChainComponent supportingCC = st.getTimeline(sdb.supporterID).getChangeNumber(sdb.supportingComponent);
            ChainComponent consumingCC = st.getTimeline(sdb.consumerID).getChainComponent(0);

            for(LogStatement sup : supportingCC.statements) {
                for(LogStatement cons : consumingCC.statements) {
                    if(areNecessarilyGlued(st, sup.end(), cons.start()) && areNecessarilyIdentical(st, sup.sv(), cons.sv())) {
                        resolvers.clear();
                        resolvers.add(res);
                        return resolvers;
                    }
                }
            }
        }

        if(planner.options.checkUnsolvableThreatsForOpenGoalsResolvers) {
            // here we check to see if some resolvers are not valid because resulting in unsolvable threats
            List<Resolver> toRemove = new LinkedList<>();
            for (Resolver res : resolvers) {
                Timeline supporter = st.getTimeline(((SupportingTimeline) res).supporterID);
                for (Timeline other : st.getTimelines()) {
                    if (other != consumer && other != supporter)
                        if (!other.hasSinglePersistence() && (st.unified(other, supporter) || st.unified(other, consumer))) {
                            // other is on the same state variable and chages the value of the state variable
                            // if it must be between the two, it will result in an unsolvable threat
                            // hence it must can be after the end of the consumer or before the start of the
                            if (!st.canAllBeBefore(consumer.getLastTimePoints(), other.getFirstChangeTimePoint())
                                    && !st.canAllBeBefore(other.getSupportTimePoint(), supporter.getFirstTimePoints())) {
                                // will result in unsolvable threat
                                toRemove.add(res);
                            }
                        }
                }
            }
            resolvers.removeAll(toRemove);
        }

        // adding actions
        // ... the idea is to decompose actions as long as they provide some support that I need, if they cant, I start adding actions
        //find actions that help me with achieving my value through some decomposition in the task netAbstractActionwork
        //they are those that I can find in the virtual decomposition tree
        //first get the action names from the abstract dtgs
        ActionSupporterFinder supporters = planner.getActionSupporterFinder();
        TaskDecompositions decompositions = new TaskDecompositions(st.pb);

        // a list of (abstract-action, decompositionID) of supporters
        Collection<fape.core.planning.preprocessing.SupportingAction> potentialSupporters = supporters.getActionsSupporting(st, consumer);

        // all actions that have an effect on the state variable
        Set<AbstractAction> potentiallySupportingAction = new HashSet<>();
        for(fape.core.planning.preprocessing.SupportingAction sa : potentialSupporters)
            potentiallySupportingAction.add(sa.absAct);

        // look for task decomposition that might produce a desired action in the future
        if(planner.isTopDownOnly()) {
            for (Task t : st.getOpenTasks()) {
                Collection<AbstractAction> decs = decompositions.possibleMethodsToDeriveTargetActions(t, potentiallySupportingAction);
                for (AbstractAction dec : decs) {
                    resolvers.add(new SupportingTaskDecomposition(t, dec, consumer));
                }
            }
        }

        //now we can look for adding the actions ad-hoc ...
        if (APlanner.actionResolvers) {
            for (fape.core.planning.preprocessing.SupportingAction aa : potentialSupporters) {
                if (planner.isTopDownOnly() && aa.absAct.mustBeMotivated())
                    continue;
                if(!st.isAddable(aa.absAct))
                    continue;

                // if the supporting variable is defined already (typically a constant) check if it is unifiable with our consumer
                AbstractLogStatement supporter = aa.absAct.jLogStatements().stream()
                        .filter(p -> p.id() == aa.statementRef).findFirst().get();
                LVarRef supportingCar = supporter instanceof AbstractAssignment
                        ? ((AbstractAssignment) supporter).value()
                        : ((AbstractTransition) supporter).to();
                if(aa.absAct.context().hasGlobalVar(supportingCar) && !st.unifiable(aa.absAct.context().getGlobalVar(supportingCar), consumer.getGlobalConsumeValue()))
                    continue;

                resolvers.add(new SupportingAction(aa.absAct, aa.statementRef, consumer));
            }
        }

        // make sure all resolvers validate the constraints built by previous resolvers.
        if(planner.isTopDownOnly()) {
            resolvers = st.retainValidResolvers(this, resolvers);
        }

        return this.resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UnsupportedTimeline;
        UnsupportedTimeline ut = (UnsupportedTimeline) o;
        return ((UnsupportedTimeline) o).consumer.mID - consumer.mID;
    }
}
