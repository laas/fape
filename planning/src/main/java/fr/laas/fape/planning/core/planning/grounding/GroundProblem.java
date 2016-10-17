package fr.laas.fape.planning.core.planning.grounding;

import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.anml.model.Function;
import fr.laas.fape.anml.model.concrete.*;
import fr.laas.fape.anml.model.concrete.statements.Assignment;
import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.anml.model.concrete.statements.Persistence;
import fr.laas.fape.anml.model.concrete.statements.Transition;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.ChainComponent;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.util.EffSet;
import lombok.Value;
import fr.laas.fape.anml.model.LVarRef;
import fr.laas.fape.anml.model.abs.AbstractAction;


import java.util.*;

public class GroundProblem {

    public final AnmlProblem liftedPb;
    public final Planner planner;

    public final List<GAction> gActions;

    final List<Invariant> invariants = new LinkedList<>();
    public final Map<IntegerInvariantKey, Integer> intInvariants = new HashMap<>();

    public class Invariant {
        public final Function f;
        public final List<InstanceRef> params;
        public final InstanceRef value;

        public Invariant(Function f, VarRef[] params, VarRef value) {
            this.f =  f;
            this.params = new LinkedList<>();
            for(VarRef v : params)
                this.params.add((InstanceRef) v);
            this.value = (InstanceRef) value;
        }

        public boolean matches(Function f, List<InstanceRef> params) {
            if(f != this.f) return false;
            assert this.params.size() == params.size();
            for(int i=0 ; i<params.size() ; i++)
                if(this.params.get(i) != params.get(i))
                    return false;
            return true;
        }
    }

    @Value public static class IntegerInvariantKey {
        public final Function f;
        public final List<InstanceRef> params;
        public IntegerInvariantKey(Function f, List<InstanceRef> params) {
            this.f = f;
            this.params = params;
        }
        public IntegerInvariantKey(Function f, VarRef[] params) {
            this.f =  f;
            this.params = new LinkedList<>();
            for(VarRef v : params) {
                this.params.add((InstanceRef) v);
            }
        }
    }

    /** Return all fluents achieved by a statement together with the timepoint at
     * which they are achieved. */
    public List<TempFluents> tempsFluents(State st) {
        if(st.fluents == null) {
            st.fluents = new LinkedList<>();
            for(Timeline db : st.getTimelines()) {
                for(ChainComponent cc : db.chain) {
                    if (cc.change) {
                        st.fluents.add(new TempFluents(DisjunctiveFluent.fluentsOf(
                                db.stateVariable,
                                cc.getSupportValue(), st, planner),
                                cc.getSupportTimePoint()));
                    }
                }
            }
        }
        return st.fluents;
    }

    /** Returns all fluents that are achieved (result of a transition or assignment) and not involved in any causal link). */
    public List<TempFluents> tempsFluentsThatCanSupportATransition(State st) {
        if(st.fluentsWithChange == null) {
            st.fluentsWithChange = new LinkedList<>();
            for(Timeline db : st.getTimelines()) {
                if(!db.hasSinglePersistence())
                    st.fluentsWithChange.add(new TempFluents(DisjunctiveFluent.fluentsOf(
                            db.stateVariable,
                            db.getGlobalSupportValue(), st, planner),
                            db.getSupportTimePoint()));
            }
        }
        return st.fluentsWithChange;
    }

    public Set<Fluent> fluentsBefore(State st, Collection<TPRef> tps) {
        Set<Fluent> fluents = new HashSet<>();
        for(TempFluents tf : tempsFluents(st)) {
            if(st.canAllBeBefore(tf.timepoints, tps))
                fluents.addAll(tf.fluents);
        }
        return fluents;
    }

    public EffSet<Fluent> allFluents(State st) {
        EffSet<Fluent> fluents = new EffSet<Fluent>(planner.preprocessor.fluentIntRepresentation());
        for(TempFluents tf : tempsFluents(st))
            fluents.addAll(tf.fluents);
        return fluents;
    }

    public GroundProblem(AnmlProblem liftedPb, Planner planner) {
        this.liftedPb = liftedPb;
        this.gActions = new LinkedList<>();
        this.planner = planner;

        for(Chronicle c : liftedPb.chronicles()) {
            for(BindingConstraint bc : c.bindingConstraints()) {
                if(bc instanceof AssignmentConstraint) {
                    AssignmentConstraint ac = (AssignmentConstraint) bc;
                    invariants.add(new Invariant(ac.sv().func(), ac.sv().args(), ac.variable()));
                }
                if(bc instanceof IntegerAssignmentConstraint) {
                    IntegerAssignmentConstraint iac = (IntegerAssignmentConstraint) bc;
                    intInvariants.put(new IntegerInvariantKey(iac.sv().func(), iac.sv().args()), iac.value());
                }
            }
        }

        for(AbstractAction liftedAct : liftedPb.abstractActions()) {
            List<GAction> grounded = GAction.groundActions(this, liftedAct, planner);
            this.gActions.addAll(grounded);
        }
    }

    public List<GAction> allActions() {
        return gActions;
    }

    protected Collection<Fluent> statementToPrecondition(LogStatement s, Map<LVarRef, InstanceRef> argMap) {
        List<Fluent> fluents = new LinkedList<>();
        GStateVariable sv = planner.preprocessor.getStateVariable(s.sv().func(), s.sv().args());
        if(s instanceof Transition) {
            fluents.add(planner.preprocessor.getFluent(sv, (InstanceRef) s.startValue()));
        } else if(s instanceof Persistence) {
            fluents.add(planner.preprocessor.getFluent(sv, (InstanceRef) s.startValue()));
        }
        return fluents;
    }

    protected Collection<Fluent> statementToAddition(LogStatement s, Map<LVarRef, VarRef> argMap) {
        List<Fluent> fluents = new LinkedList<>();
        if(s instanceof Transition || s instanceof Assignment) {
            GStateVariable sv = planner.preprocessor.getStateVariable(s.sv().func(), s.sv().args());
            fluents.add(planner.preprocessor.getFluent(sv, (InstanceRef) s.endValue()));
        }
        return fluents;
    }
}
