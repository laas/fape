package fr.laas.fape.planning.core.planning.search.flaws.flaws;

import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.planner.PlanningOptions;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.SupportingAction;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.SupportingTimeline;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.ChainComponent;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.exceptions.FAPEException;
import lombok.EqualsAndHashCode;
import lombok.Value;
import fr.laas.fape.anml.model.LVarRef;
import fr.laas.fape.anml.model.abs.statements.AbstractLogStatement;

import java.util.*;

@Value @EqualsAndHashCode(callSuper = false)
public class UnsupportedTimeline extends Flaw {

    public final Timeline consumer;
    public final PlanningOptions.ActionInsertionStrategy actionInsertionStrategy;

    @Override
    public String toString() {
        return "Unsupported: " + consumer;
    }

    @Override
    public List<Resolver> getResolvers(State st, Planner planner) {
        if(this.resolvers == null)
            this.resolvers = st.getResolversForOpenGoal(consumer, actionInsertionStrategy);
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
    private static boolean isSupporting(Timeline potentialSupporter, int changeNumber, Timeline consumer, State st) {
        if(consumer == potentialSupporter)
            return false;

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

    private static boolean isValid(SupportingAction supportingAction, Timeline consumer, State st) {
        assert consumer.mID == supportingAction.consumerID;

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
        }

        for(int i=0 ; i<supporter.sv().args().size() ; i++) {
            LVarRef lv = supporter.sv().jArgs().get(i);
            VarRef v = consumer.stateVariable.arg(i);
            if(supportingAction.act.context().hasGlobalVar(lv)) {
                if(!st.unifiable(supportingAction.act.context().getGlobalVar(lv), v))
                    return false;
            }
        }

        return true;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof UnsupportedTimeline;
        return ((UnsupportedTimeline) o).consumer.mID - consumer.mID;
    }
}
