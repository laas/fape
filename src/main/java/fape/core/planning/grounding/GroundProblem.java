package fape.core.planning.grounding;

import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.PGUtils;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.FAPEException;
import fape.util.EffSet;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.Function;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.Assignment;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;
import planstack.anml.model.concrete.statements.Transition;

import java.util.*;

public class GroundProblem {

    public final AnmlProblem liftedPb;
    public final APlanner planner;
    public final GroundState initState = new GroundState();
    public final GroundState goalState = new GroundState();

    public final List<GAction> gActions;

    final List<Invariant> invariants = new LinkedList<>();
    final Map<IntegerInvariantKey, Integer> intInvariants = new HashMap<>();

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

    public class IntegerInvariantKey {
        public final Function f;
        public final List<InstanceRef> params;
        public int hash;
        public IntegerInvariantKey(Function f, VarRef[] params) {
            this.f =  f;
            this.params = new LinkedList<>();
            int i = 0;
            hash = f.hashCode();
            for(VarRef v : params) {
                this.params.add((InstanceRef) v);
                hash += (++i) * 42 * v.hashCode();
            }
        }
        public int hashCode() { return hash; }
        public boolean equals(Object o) {
            if(!(o instanceof IntegerInvariantKey))
                return false;
            if(!(((IntegerInvariantKey) o).f == f))
                return false;
            for(int i=0 ; i<params.size() ; i++)
                if(!params.get(i).equals(((IntegerInvariantKey) o).params.get(i)))
                    return false;
            return true;
        }
    }


    private List<TempFluents> tempsFluents(State st) {
        if(st.fluents == null) {
            st.fluents = new LinkedList<>();
            for(Timeline db : st.getTimelines()) {
                for(ChainComponent cc : db.chain) {
                    if (cc.change) {
                        // those values can be used for persistences but not for transitions.
                        st.fluents.add(new TempFluents(DisjunctiveFluent.fluentsOf(db.stateVariable, cc.getSupportValue(), st, planner), cc.getSupportTimePoint()));
                    }
                }
            }
        }
        return st.fluents;
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

    public GroundProblem(AnmlProblem liftedPb, APlanner planner) {
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
            this.gActions.addAll(GAction.groundActions(this, liftedAct, planner));
        }

        for(Chronicle mod : liftedPb.chronicles()) {
            for(LogStatement s : mod.logStatements()) {
                for(Fluent addition : statementToAddition(s, null))
                    initState.fluents.add(addition);
                for(Fluent precondition : statementToPrecondition(s, null))
                    goalState.fluents.add(precondition);
            }
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
