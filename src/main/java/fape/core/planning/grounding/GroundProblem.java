package fape.core.planning.grounding;

import fape.core.planning.planninggraph.PGUtils;
import fape.core.planning.states.State;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
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

    private static List<TempFluents> tempsFluents(State st) {
        if(st.fluents == null) {
            st.fluents = new LinkedList<>();
            for(Timeline db : st.getTimelines()) {
                for(ChainComponent cc : db.chain) {
                    if (cc.change) {
                        // those values can be used for persistences but not for transitions.
                        st.fluents.add(new TempFluents(DisjunctiveFluent.fluentsOf(db.stateVariable, cc.getSupportValue(), st, false), cc.getSupportTimePoint()));
                    }
                }
            }
        }
        return st.fluents;
    }

    public static Set<Fluent> fluentsBefore(State st, Collection<TPRef> tps) {
        Set<Fluent> fluents = new HashSet<>();
        for(TempFluents tf : tempsFluents(st)) {
            if(st.canAllBeBefore(tf.timepoints, tps))
                fluents.addAll(tf.fluents);
        }
        return fluents;
    }

    public static Set<Fluent> allFluents(State st) {
        Set<Fluent> fluents = new HashSet<>();
        for(TempFluents tf : tempsFluents(st))
            fluents.addAll(tf.fluents);
        return fluents;
    }


    public GroundProblem(GroundProblem pb, State st) {
        this.liftedPb = pb.liftedPb;
        this.gActions = new LinkedList<>(pb.gActions);

        initState.fluents.addAll(allFluents(st));
    }

    public GroundProblem(GroundProblem pb, State st, Timeline og) {
        this.liftedPb = pb.liftedPb;
        this.gActions = pb.gActions;

        initState.fluents.addAll(fluentsBefore(st, og.getFirstTimePoints()));
    }

    public GroundProblem(AnmlProblem liftedPb) {
        this.liftedPb = liftedPb;
        this.gActions = new LinkedList<>();

        for(Chronicle c : liftedPb.chronicles()) {
            for(BindingConstraint bc : c.bindingConstraints()) {
                if(bc instanceof AssignmentConstraint) {
                    AssignmentConstraint ac = (AssignmentConstraint) bc;
                    invariants.add(new Invariant(ac.sv().func(), ac.sv().args(), ac.variable()));
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
            fluents.add(new Fluent(s.sv().func(), s.sv().args(), s.endValue(), true));
        } else if(s instanceof Persistence) {
            fluents.add(new Fluent(s.sv().func(), s.sv().args(), s.startValue(), false));
        }
        return fluents;
    }

    protected Collection<Fluent> statementToAddition(LogStatement s, Map<LVarRef, VarRef> argMap) {
        List<Fluent> fluents = new LinkedList<>();
        if(s instanceof Transition || s instanceof Assignment) {
            fluents.add(new Fluent(s.sv().func(), s.sv().args(), s.endValue(), false));
            fluents.add(new Fluent(s.sv().func(), s.sv().args(), s.endValue(), true));
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
                    fluents.add(new Fluent(s.sv().func(), s.sv().args(), val, true));
                    fluents.add(new Fluent(s.sv().func(), s.sv().args(), val, false));
                }
            }
        } else {
            if(s.startValue() != s.endValue()) {
                fluents.add(new Fluent(s.sv().func(), s.sv().args(), s.startValue(), true));
                fluents.add(new Fluent(s.sv().func(), s.sv().args(), s.startValue(), false));
            }
        }
        return fluents;
    }
}
