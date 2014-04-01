package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;

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
    final GroundState initState = new GroundState();

    final List<GroundAction> actions = new LinkedList<>();

    public GroundProblem(AnmlProblem liftedPb) {
        this.liftedPb = liftedPb;

        for(AbstractAction liftedAct : liftedPb.jAbstractActions()) {
            // ignore methods with decompositions
            if(!liftedAct.jDecompositions().isEmpty())
                continue;

            List<List<VarRef>> paramsLists = possibleParams(liftedAct);
            for(List<VarRef> params : paramsLists) {
                GroundAction candidate = new GroundAction(liftedAct, params, this);

                if(candidate.isValid()) {
                    actions.add(candidate);
                } else {
                    System.out.println("Ignored: " + candidate);
                }
            }
        }

        for(StateModifier mod : liftedPb.jModifiers()) {
            for(LogStatement s : mod.logStatements()) {
                boolean isOnStart = false;
                for(TemporalConstraint constraint : mod.temporalConstraints()) {
                    if(constraint.op().equals("=") && constraint.plus() == 0) {
                        // this is an equality constraint
                        if(constraint.tp1() == s.end() && constraint.tp2() == liftedPb.start() ||
                                constraint.tp2() == s.end() && constraint.tp1() == liftedPb.start()) {
                            isOnStart = true;
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
            }
        }
    }

    public List<GroundAction> allActions() {
        return actions;
    }

    public List<List<VarRef>> possibleParams(AbstractAction a) {
        // the ith list contains the possible values for the ith parameter
        List<List<VarRef>> possibleValues = new LinkedList<>();
        for(LVarRef ref : JavaConversions.asJavaCollection(a.args())) {
            // get type of the argument and add all possible values to the argument list.
            List<String> instanceSet = liftedPb.instances().instancesOfType(a.context().getType(ref));
            List<VarRef> varSet = new LinkedList<>();
            for(String instance : instanceSet) {
                varSet.add(liftedPb.instances().referenceOf(instance));
            }
            possibleValues.add(varSet);
        }

        return allCombinations(possibleValues, 0, new LinkedList<VarRef>());


    }

    /**
     * [[a1, a2], [b], [c1, C2]]
     *  => [ [a1, b, c1], [a1, b, c2], [a2, b, c1], [a2, b, c2]]
     * @param valuesSets
     * @param startWith
     * @return
     */
    public List<List<VarRef>> allCombinations(List<List<VarRef>> valuesSets, int startWith, List<VarRef> baseValues) {
        List<List<VarRef>> ret = new LinkedList<>();

        if(startWith >= valuesSets.size()) {
            ret.add(baseValues);
            return ret;
        }
        for(VarRef val : valuesSets.get(startWith)) {
            List<VarRef> newBaseValues = new LinkedList<>(baseValues);
            newBaseValues.add(val);
            ret.addAll(allCombinations(valuesSets, startWith+1, newBaseValues));
        }

        return ret;
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

    protected Fluent statementToDeletion(LogStatement s, Map<LVarRef, VarRef> argMap) {
        if(!(s instanceof Assignment || s instanceof Transition)) {
            return null;
        } else if(s instanceof Assignment) {
            throw new FAPEException("Error: creating delete list from assignment is not supported");
        } else {
            return new Fluent(s.sv().func(), s.sv().jArgs(), s.startValue());
        }
    }
}
