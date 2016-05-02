package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.Planner;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.TaskDecompositionsReasoner;
import fape.core.planning.search.flaws.resolvers.*;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.FAPEException;
import planstack.anml.model.LVarRef;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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



    @Override
    public List<Resolver> getResolvers(State st, Planner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new ArrayList<>();

        //here we need to find several types of supporters
        //1) chain parts that provide the value we need
        //2) actions that provide the value we need and can be added
        //3) tasks that can decompose into an action we need

        // get the resolvers that are cached in the state: SupportingTimelines and SupportingActions
        for(Resolver r : st.getResolversForOpenGoal(consumer))
            resolvers.add(r);

        // checks all the given resolvers to check if one must be applied.
        // this is true iff the supporter is non separable from the consumer:
        //    they are necessarily on the same state variable and they cannot be temporally separated.
        // If such a resolver if found, it means that no other resolver is applicable and only this one is returned.
        for(Resolver res : resolvers) {
            if(!(res instanceof SupportingTimeline))
                continue;
            SupportingTimeline sdb = (SupportingTimeline) res;
            ChainComponent supportingCC = st.getTimeline(sdb.supporterID).getChangeNumber(sdb.supportingComponent);
            ChainComponent consumingCC = st.getTimeline(sdb.consumerID).getChainComponent(0);

            for(LogStatement sup : supportingCC.statements) {
                for(LogStatement cons : consumingCC.statements) {
                    if(areNecessarilyGlued(st, sup.end(), cons.start()) && areNecessarilyIdentical(st, sup.sv(), cons.sv())) {
                        resolvers = Collections.singletonList(res);
                        return resolvers;
                    }
                }
            }
        }

        if(st.pb.allActionsAreMotivated()) {
            ActionSupporterFinder supporters = planner.getActionSupporterFinder();
            TaskDecompositionsReasoner decompositions = st.pl.preprocessor.getTaskDecompositionsReasoner();

            // a list of (abstract-action, decompositionID) of supporters
            Collection<fape.core.planning.preprocessing.SupportingAction> potentialSupporters = supporters.getActionsSupporting(st, consumer);

            // all actions that have an effect on the state variable
            Set<AbstractAction> potentiallySupportingAction = potentialSupporters.stream()
                    .map(x -> x.absAct)
                    .collect(Collectors.toSet());

            Set<Task> tasks = new HashSet<>();
            assert !st.getHierarchicalConstraints().isWaitingForADecomposition(consumer);

            for (Task t : st.getOpenTasks()) {
                if (!st.canBeStrictlyBefore(t.start(), consumer.getConsumeTimePoint()))
                    continue;
                if (!st.getHierarchicalConstraints().isValidTaskSupport(t, consumer))
                    continue;

                Collection<AbstractAction> decs = decompositions.possibleMethodsToDeriveTargetActions(t, potentiallySupportingAction);
                if (!decs.isEmpty())
                    tasks.add(t);
            }

            // resolvers are only existing statements that validate the constraints and the future task supporters we found
            List<Resolver> newRes = Stream.concat(
                    resolvers.stream()
                            .filter(resolver -> resolver instanceof SupportingTimeline)
                            .map(resolver1 -> (SupportingTimeline) resolver1)
                            .filter(res -> st.getHierarchicalConstraints().isValidSupport(res.getSupportingStatement(st), consumer)),
                    tasks.stream()
                            .map(t -> new FutureTaskSupport(consumer, t)))
                    .collect(Collectors.toList());

            resolvers = newRes;
        }

        return this.resolvers;
    }

    public static boolean isValidResolver(Resolver res, Timeline consumer, State st) {
        if(res instanceof SupportingAction) {
            return isValid((SupportingAction) res, consumer, st);
        } else if(res instanceof SupportingTimeline) {
            SupportingTimeline supportingTimeline = (SupportingTimeline) res;
            if(!st.containsTimelineWithID(supportingTimeline.supporterID))
                return false; // timeline was deleted since the last check
            else
                return isSupporting(st.getTimeline(supportingTimeline.supporterID), supportingTimeline.supportingComponent, consumer, st);
        } else {
            throw new FAPEException("Unsupported resolver: "+res);
        }
    }

    /** Returns true if the nth change in potentialSupporter (n = changeNumber) can support the consumer timeline */
    public static boolean isSupporting(Timeline potentialSupporter, int changeNumber, Timeline consumer, State st) {
        if(consumer == potentialSupporter)
            return false; //TODO: this should not be necessary to avoid the assertion since they should be temporally distinct

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

        assert potentialSupporter != consumer;

        return true;
    }

    public static boolean isValid(SupportingAction supportingAction, Timeline consumer, State st) {
        assert consumer.mID == supportingAction.consumerID;

        Planner planner = st.pl;
        if(!st.isAddable(supportingAction.act))
            return false;

        // if the supporting variable is defined already (typically a constant) check if it is unifiable with our consumer
        AbstractLogStatement supporter = supportingAction.act.getLogStatement(supportingAction.statementRef);
        assert supporter.id().equals(supportingAction.statementRef);
        LVarRef supportingVar = supporter.effectValue();
        VarRef consumerVar = consumer.getGlobalConsumeValue();

        if(supportingAction.act.context().hasGlobalVar(supportingVar)) {
            if(!st.unifiable(supportingAction.act.context().getGlobalVar(supportingVar), consumerVar))
                return false;
        } else {
            // this is quite expensive but can prove useful on some domains
//            Set<String> futureDom = new HashSet<>(st.pb.instances().instancesOfType(supportingVar.getType()));
//            List<String> dom = st.csp.bindings().domainOf(consumerVar);
//            futureDom.retainAll(dom);
//            if(futureDom.isEmpty())
//                return false;
        }

        for(int i=0 ; i<supporter.sv().args().size() ; i++) {
            LVarRef lv = supporter.sv().jArgs().get(i);
            VarRef v = consumer.stateVariable.arg(i);
            if(supportingAction.act.context().hasGlobalVar(lv)) {
                if(!st.unifiable(supportingAction.act.context().getGlobalVar(lv), v))
                    return false;
            } else {
                // this is quite expensive but can prove useful on some domains
//                Set<String> futureDom = new HashSet<>(st.pb.instances().instancesOfType(lv.getType()));
//                List<String> dom = st.csp.bindings().domainOf(v);
//                futureDom.retainAll(dom);
//                if(futureDom.isEmpty())
//                    return false;
            }
        }

        return true;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UnsupportedTimeline;
        UnsupportedTimeline ut = (UnsupportedTimeline) o;
        return ((UnsupportedTimeline) o).consumer.mID - consumer.mID;
    }
}
