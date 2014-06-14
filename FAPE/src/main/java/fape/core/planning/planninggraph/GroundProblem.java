package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import planstack.anml.model.concrete.statements.*;
import planstack.anml.model.concrete.StateModifier;
import planstack.anml.model.concrete.TemporalConstraint;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.Statement;
import scala.collection.JavaConversions;

public class GroundProblem {

    final AnmlProblem liftedPb;
    public final GroundState initState = new GroundState();
    public final GroundState goalState = new GroundState();

    final List<GroundAction> actions = new LinkedList<>();

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

    public GroundProblem(AnmlProblem liftedPb) {
        this.liftedPb = liftedPb;

        for(AbstractAction liftedAct : liftedPb.abstractActions()) {
            // ignore methods with decompositions
            if(!liftedAct.jDecompositions().isEmpty())
                continue;

            List<List<VarRef>> paramsLists = possibleParams(liftedAct);
            for(List<VarRef> params : paramsLists) {
                GroundAction candidate = new GroundAction(liftedAct, params, this);

                if(candidate.isValid()) {
                    actions.add(candidate);
                } else {
                    //System.out.println("Ignored: " + candidate);
                }
            }
        }

        for(StateModifier mod : liftedPb.modifiers()) {
            for(LogStatement s : mod.logStatements()) {/*
                boolean isOnStart = false;
                boolean isOnEnd = false;
                for(TemporalConstraint constraint : mod.temporalConstraints()) {
                    if(constraint.op().equals("=") && constraint.plus() == 0) {
                        // this is an equality constraint
                        if(constraint.tp1() == s.end() && constraint.tp2() == liftedPb.start() ||
                                constraint.tp2() == s.end() && constraint.tp1() == liftedPb.start()) {
                            isOnStart = true;
                            break;
                        }
                        if(constraint.tp1() == s.start() && constraint.tp2() == liftedPb.end() ||
                                constraint.tp2() == s.start() && constraint.tp1() == liftedPb.end()) {
                            isOnEnd = true;
                            break;
                        }
                    }
                }
                if(isOnStart) {
                    assert statementToPrecondition(s, null) == null;
                    Fluent addition = statementToAddition(s, null);
                    if(addition != null)
                        initState.fluents.add(addition);
                }
                if(isOnEnd) {
                    Fluent precondition = statementToPrecondition(s, null);
                    if(precondition != null)
                        goalState.fluents.add(precondition);
                }*/
                Fluent addition = statementToAddition(s, null);
                if(addition != null)
                    initState.fluents.add(addition);
                Fluent precondition = statementToPrecondition(s, null);
                if(precondition != null)
                    goalState.fluents.add(precondition);
            }
        }
    }

    public List<GroundAction> allActions() {
        return actions;
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

    protected Fluent statementToPrecondition(LogStatement s, Map<LVarRef, VarRef> argMap) {
        if(!(s instanceof Persistence || s instanceof Transition)) {
            return null;
        } else {
            return new Fluent(s.sv().func(), s.sv().jArgs(), s.startValue());
        }
    }

    protected Fluent statementToAddition(LogStatement s, Map<LVarRef, VarRef> argMap) {
        if(!(s instanceof Assignment || s instanceof Transition)) {
            return null;
        } else {
            return new Fluent(s.sv().func(), s.sv().jArgs(), s.endValue());
        }
    }

    protected Collection<Fluent> statementToDeletions(LogStatement s, Map<LVarRef, VarRef> argMap) {
        List<Fluent> fluents = new LinkedList<>();
        if(!(s instanceof Assignment || s instanceof Transition)) {
        } else if(s instanceof Assignment) {
            for(String value : liftedPb.instances().instancesOfType(s.sv().func().valueType())) {
                VarRef val = liftedPb.instances().referenceOf(value);
                if(val != s.endValue()) {
                    fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), val));
                }
            }
        } else {
            fluents.add(new Fluent(s.sv().func(), s.sv().jArgs(), s.startValue()));
        }
        return fluents;
    }
}
