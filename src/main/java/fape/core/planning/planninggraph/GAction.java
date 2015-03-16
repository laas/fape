package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.AbstractParameterizedStateVariable;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.*;
import planstack.anml.model.concrete.EmptyVarRef;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.EqualityConstraint;
import planstack.anml.model.concrete.statements.Persistence;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GAction implements PGNode {

    public List<Fluent> pre = new LinkedList<>();
    public List<Fluent> add = new LinkedList<>();
    public final AbstractAction abs;
    public final String name;

    public GroundProblem.Invariant invariantOf(AbstractParameterizedStateVariable sv, Map<LVarRef, InstanceRef> vars, GroundProblem gPb) {
        List<InstanceRef> params = new LinkedList<>();
        for(LVarRef v : sv.jArgs())
            params.add(valueOf(v, vars, gPb.liftedPb));
        for(GroundProblem.Invariant inv : gPb.invariants) {
            if(inv.matches(sv.func(), params))
                return inv;
        }
        return null;
    }

    public GAction(AbstractAction abs, Map<LVarRef, InstanceRef> vars, GroundProblem gPb) {
        AnmlProblem pb = gPb.liftedPb;
        this.abs = abs;

        for(AbstractStatement as : abs.jTemporalStatements()) {
            if(as instanceof AbstractEqualityConstraint) {
                AbstractEqualityConstraint ec = (AbstractEqualityConstraint) as;
                GroundProblem.Invariant inv = invariantOf(ec.sv(), vars, gPb);
                if(inv == null || inv.value != valueOf(ec.variable(), vars, pb)) {
                    throw new FAPEException("Action not valid");
                }
            } else if(as instanceof AbstractInequalityConstraint) {
                AbstractInequalityConstraint ec = (AbstractInequalityConstraint) as;
                GroundProblem.Invariant inv = invariantOf(ec.sv(), vars, gPb);
                if(inv == null || inv.value == valueOf(ec.variable(), vars, pb)) {
                    throw new FAPEException("Action not valid");
                }
            } else if(as instanceof AbstractVarEqualityConstraint) {
                AbstractVarEqualityConstraint ec = (AbstractVarEqualityConstraint) as;
                if(valueOf(ec.leftVar(), vars, pb) != valueOf(ec.rightVar(), vars, pb))
                    throw new FAPEException("Action not valid");
            } else if(as instanceof AbstractVarInequalityConstraint) {
                AbstractVarInequalityConstraint ec = (AbstractVarInequalityConstraint) as;
                if(valueOf(ec.leftVar(), vars, pb) == valueOf(ec.rightVar(), vars, pb))
                    throw new FAPEException("Action not valid");
            }
        }



        for(AbstractLogStatement s : abs.jLogStatements()) {
            if(s instanceof AbstractTransition) {
                AbstractTransition t = (AbstractTransition) s;
                pre.add(fluent(t.sv(), t.from(), vars, pb));
                add.add(fluent(t.sv(), t.to(), vars, pb));
            } else if(s instanceof AbstractPersistence) {
                AbstractPersistence p = (AbstractPersistence) s;
                pre.add(fluent(p.sv(), p.value(), vars, pb));
            } else if(s instanceof AbstractAssignment) {
                AbstractAssignment a = (AbstractAssignment) s;
                add.add(fluent(a.sv(), a.value(), vars, pb));
            }
        }

        String ret = "";
        ret += abs.name()+"(";
        for(int i=0 ; i<abs.args().size() ; i++) {
            ret += valueOf(abs.args().get(i), vars, pb);
            if(i < abs.args().size()-1)
                ret += ", ";
        }
        ret +=") ";
        for(Map.Entry<LVarRef,InstanceRef> binding : vars.entrySet()) {
            ret += binding.getKey() + ":"+binding.getValue()+" ";
        }
        this.name = ret;
    }

    @Override
    public String toString() { return name; }

    public InstanceRef valueOf(LVarRef var, Map<LVarRef, InstanceRef> vars, AnmlProblem pb) {
        InstanceRef ret;
        if(vars.containsKey(var)) {
            ret = vars.get(var);
        } else {
            ret = (InstanceRef) pb.context().getDefinition(var)._2();
        }
        return ret;
    }

    public Fluent fluent(AbstractParameterizedStateVariable sv, LVarRef value, Map<LVarRef, InstanceRef> vars, AnmlProblem pb) {
        List<VarRef> svParams = new LinkedList<>();
        for(LVarRef v : sv.jArgs()) {
            svParams.add(valueOf(v, vars, pb));
        }
        return new Fluent(sv.func(), svParams, valueOf(value, vars, pb));
    }

    public static List<GAction> groundActions(GroundProblem gPb, AbstractAction aa) {
        AnmlProblem pb = gPb.liftedPb;

        List<LVarRef> vars = new LinkedList<>();
        List<List<InstanceRef>> possibleValues = new LinkedList<>();
        for(LVarRef ref : scala.collection.JavaConversions.asJavaIterable(aa.context().variables().keys())) {
            vars.add(ref);
            List<InstanceRef> varSet = new LinkedList<>();
            if(!aa.context().getDefinition(ref)._2().isEmpty()) {
                if(!(aa.context().getDefinition(ref)._2() instanceof InstanceRef)) {
                    System.out.print("ERRROR: "+aa.context().getDefinition(ref)._2());
                }
                varSet.add((InstanceRef) aa.context().getDefinition(ref)._2());
            } else {
                // get type of the argument and add all possible values to the argument list.
                List<String> instanceSet = pb.instances().instancesOfType(aa.context().getType(ref));
                for (String instance : instanceSet) {
                    varSet.add(pb.instances().referenceOf(instance));
                }
            }
            possibleValues.add(varSet);
        }

        List<List<InstanceRef>> instanciations = PGUtils.allCombinations(possibleValues);
        List<Map<LVarRef, InstanceRef>> paramsLists = new LinkedList<>();

        for(List<InstanceRef> instanciation : instanciations) {
            Map<LVarRef, InstanceRef> params = new HashMap<>();
            assert instanciation.size() == vars.size();
            for(int i=0 ; i< vars.size() ; i++) {
                params.put(vars.get(i), instanciation.get(i));
            }
            paramsLists.add(params);
        }

        List<GAction> actions = new LinkedList<>();
        for(Map<LVarRef, InstanceRef> params : paramsLists) {
            try {
                actions.add(new GAction(aa, params, gPb));
            } catch (FAPEException e) {
                //not valid
            }
        }

        return actions;
    }
}
