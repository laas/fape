package fape.core.planning.planninggraph;

import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.exceptions.FAPEException;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.Function;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Chronicle;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.*;
import planstack.anml.parser.ANMLFactory;

import java.util.*;

public class GroundProblem {

    public final AnmlProblem liftedPb;
    public final GroundState initState = new GroundState();
    public final GroundState goalState = new GroundState();

    public final List<GAction> gActions;

    final List<Invariant> invariants = new LinkedList<>();

    public class Invariant {
        public final Function f;
        public final List<InstanceRef> params;
        public final InstanceRef value;

        public Invariant(Function f, List<VarRef> params, VarRef value) {
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

    /**
     * Gets an upper bound on the number of ground actions that might appear in this problem.
     * @param pb Problem to inspect.
     * @return Upper on the number of ground actions.
     */
    public static int sizeEvaluation(AnmlProblem pb) {
        int total = 0;
        for(AbstractAction a : pb.abstractActions()) {
            int nemActInstances = 1;
            for(LVarRef arg : a.args()) {
                String argTYpe = a.context().getType(arg);
                nemActInstances *= pb.instances().instancesOfType(argTYpe).size();
            }
            total += nemActInstances;
        }
        return total;
    }

    Collection<Fluent> dbToFluents(TemporalDatabase db, State st) {
        HashSet<Fluent> fluents = new HashSet<>();
        for(ChainComponent cc : db.chain) {
            if (cc.change) {
                // those values can be used for persistences but not for transitions.
                fluents.addAll(DisjunctiveFluent.fluentsOf(db.stateVariable, cc.GetSupportValue(), st, false));
            }
        }
        // the last value can be used for transitions as well
        if(!db.HasSinglePersistence())
            fluents.addAll(DisjunctiveFluent.fluentsOf(db.stateVariable, db.GetGlobalSupportValue(), st, true));
        return fluents;
    }

    public GroundProblem(GroundProblem pb, State st) {
        this.liftedPb = pb.liftedPb;
        this.gActions = new LinkedList<>(pb.gActions);

        for(TemporalDatabase db : st.tdb.vars) {
            initState.fluents.addAll(dbToFluents(db, st));
        }
    }

    public GroundProblem(GroundProblem pb, State st, TemporalDatabase og) {
        this.liftedPb = pb.liftedPb;
        this.gActions = pb.gActions;

        for(TemporalDatabase db : st.tdb.vars) {
            if(db.HasSinglePersistence())
                continue;
            for(ChainComponent cc : db.chain) {
                if(cc.change && canIndirectlySupport(st, cc, og)) {
                    initState.fluents.addAll(DisjunctiveFluent.fluentsOf(db.stateVariable, cc.GetSupportValue(), st, false));
                }
            }
            if(st.canBeBefore(db.getSupportTimePoint(), og.getFirstTimePoints()))
                initState.fluents.addAll(DisjunctiveFluent.fluentsOf(db.stateVariable, db.GetGlobalSupportValue(), st, true));
        }

    }

    public boolean canIndirectlySupport(State st, ChainComponent supporter, TemporalDatabase consumer) {
        assert supporter.change;

        for(TPRef consumeTP : consumer.getFirstTimePoints()) {
            if(!st.canBeBefore(supporter.getSupportTimePoint(), consumeTP))
                return false;
        }
        return true;
    }

    public GroundProblem(AnmlProblem liftedPb) {
        this.liftedPb = liftedPb;
        this.gActions = new LinkedList<>();

        for(Chronicle c : liftedPb.chronicles()) {
            for(BindingConstraint bc : c.bindingConstraints()) {
                if(bc instanceof AssignmentConstraint) {
                    AssignmentConstraint ac = (AssignmentConstraint) bc;
                    invariants.add(new Invariant(ac.sv().func(), ac.sv().jArgs(), ac.variable()));
                }
            }
        }

        for(AbstractAction liftedAct : liftedPb.abstractActions()) {
            this.gActions.addAll(GAction.groundActions(this, liftedAct));
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

    public List<List<VarRef>> possibleParams(AbstractAction a) {
        // the ith list contains the possible values for the ith parameter
        List<List<VarRef>> possibleValues = new LinkedList<>();
        for(LVarRef ref : a.args()) {
            // get type of the argument and add all possible values to the argument list.
            List<String> instanceSet = liftedPb.instances().instancesOfType(a.context().getType(ref));
            List<VarRef> varSet = new LinkedList<>();
            for(String instance : instanceSet) {
                varSet.add(liftedPb.instances().referenceOf(instance));
            }
            possibleValues.add(varSet);
        }

        return PGUtils.allCombinations(possibleValues);
    }



    String valueOf(VarRef var) {
        for(String instance : liftedPb.instances().allInstances()) {
            if(liftedPb.instances().referenceOf(instance).equals(var)) {
                return instance;
            }
        }
        throw new FAPEException("Unable to find the instance referred to by "+var);
    }

    /**
     * Finds which instance correspond to the given local variable reference.
     * @param ref Reference to the variable to lookup.
     * @param argMap A map from local variables to instances (those typically represent the parameters given to an action.
     * @return The instance the local reference maps to.
     */
    public VarRef getInstance(LVarRef ref, Map<LVarRef, VarRef> argMap) {
        if(argMap == null || !argMap.containsKey(ref)) {
            assert liftedPb.instances().containsInstance(ref.id());
            return liftedPb.instances().referenceOf(ref.id());
        } else {
            return argMap.get(ref);
        }
    }

    protected Collection<Fluent> statementToPrecondition(LogStatement s, Map<LVarRef, VarRef> argMap) {
        List<Fluent> fluents = new LinkedList<>();
        if(s instanceof Transition) {
            fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), s.endValue(), true));
        } else if(s instanceof Persistence) {
            fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), s.startValue(), false));
        }
        return fluents;
    }

    protected Collection<Fluent> statementToAddition(LogStatement s, Map<LVarRef, VarRef> argMap) {
        List<Fluent> fluents = new LinkedList<>();
        if(s instanceof Transition || s instanceof Assignment) {
            fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), s.endValue(), false));
            fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), s.endValue(), true));
        }
        return fluents;
    }

    protected Collection<Fluent> statementToDeletions(LogStatement s, Map<LVarRef, VarRef> argMap) {
        List<Fluent> fluents = new LinkedList<>();
        if(!(s instanceof Assignment || s instanceof Transition)) {
        } else if(s instanceof Assignment) {
            for(String value : liftedPb.instances().instancesOfType(s.sv().func().valueType())) {
                VarRef val = liftedPb.instances().referenceOf(value);
                if(val != s.endValue()) {
                    fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), val, true));
                    fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), val, false));
                }
            }
        } else {
            if(s.startValue() != s.endValue()) {
                fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), s.startValue(), true));
                fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), s.startValue(), false));
            }
        }
        return fluents;
    }
}
