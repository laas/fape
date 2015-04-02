package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.AbstractParameterizedStateVariable;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractActionRef;
import planstack.anml.model.abs.statements.*;
import planstack.anml.model.concrete.EmptyVarRef;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.EqualityConstraint;
import planstack.anml.model.concrete.statements.Persistence;
import planstack.anml.parser.Instance;
import planstack.structures.Pair;
import scala.collection.JavaConversions;

import java.util.*;

public class GAction implements PGNode {

    public List<Fluent> pre = new LinkedList<>();
    public List<Fluent> add = new LinkedList<>();
    public final AbstractAction abs;
//    public final String name;
    public final LVarRef[] vars;
    protected final InstanceRef[] values;
    public final int decID;
    public final ArrayList<GTaskCond> subTasks;

    private static int nextID = 0;
    public final int id;

    public InstanceRef valueOf(LVarRef v) {
        for(int i=0 ; i<vars.length ; i++)
            if(vars[i].equals(v))
                return values[i];
        throw new FAPEException("This local variable was not found: "+v);
    }

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

    public GAction(AbstractAction abs, int decID, Map<LVarRef, InstanceRef> vars, GroundProblem gPb) {
        AnmlProblem pb = gPb.liftedPb;
        this.abs = abs;
        assert decID < abs.jDecompositions().size();
        this.decID = decID;

        this.vars = new LVarRef[vars.size()];
        this.values = new InstanceRef[vars.size()];
        int i=0;
        for(Map.Entry<LVarRef,InstanceRef> binding : vars.entrySet()) {
            this.vars[i] = binding.getKey();
            this.values[i] = binding.getValue();
            i++;
        }

        List<AbstractStatement> statements;
        if(decID == -1) {
            statements = abs.jTemporalStatements();
        } else {
            statements = new LinkedList<>(abs.jTemporalStatements());
            statements.addAll(abs.jDecompositions().get(decID).jStatements());
        }

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
                pre.add(fluent(t.sv(), t.from(), true, vars, pb));
                pre.add(fluent(t.sv(), t.from(), false, vars, pb));
                add.add(fluent(t.sv(), t.to(), true, vars, pb));
                add.add(fluent(t.sv(), t.to(), false, vars, pb));
            } else if(s instanceof AbstractPersistence) {
                AbstractPersistence p = (AbstractPersistence) s;
                pre.add(fluent(p.sv(), p.value(), false, vars, pb));
            } else if(s instanceof AbstractAssignment) {
                AbstractAssignment a = (AbstractAssignment) s;
                add.add(fluent(a.sv(), a.value(), false, vars, pb));
                add.add(fluent(a.sv(), a.value(), true, vars, pb));
            }
        }

        // with temporal actions, a lot of actions can be self suportive
        pre.removeAll(add);

        this.id = nextID++;
        this.subTasks = initSubTasks(gPb.liftedPb);
    }

    @Override
    public String toString() {
        String ret = "("+id+")";
        ret += abs.name()+ (decID == -1 ? "" : "-"+decID) + "(";
        for(int j=0 ; j<abs.args().size() ; j++) {
            ret += valueOf(abs.args().get(j));
            if(j < abs.args().size()-1)
                ret += ", ";
        }
        ret +=") ";
        for(int i=0 ; i<vars.length ; i++) {
            if(!abs.args().contains(vars[i]))
                ret += vars[i] +":"+ values[i]+" ";
        }
        return ret;
    }

    public String toASP() {
        String ret = "";
        ret += abs.name().toLowerCase()+ (decID == -1 ? "" : "_"+decID) + "__";
        for(int i=0 ; i<vars.length ; i++) {
            ret += vars[i] +"_"+ values[i]+"__";
        }
        return ret;
    }

    public InstanceRef valueOf(LVarRef var, Map<LVarRef, InstanceRef> vars, AnmlProblem pb) {
        InstanceRef ret;
        if(vars.containsKey(var)) {
            ret = vars.get(var);
        } else {
            ret = (InstanceRef) pb.context().getDefinition(var)._2();
        }
        return ret;
    }

    public Fluent fluent(AbstractParameterizedStateVariable sv, LVarRef value, boolean partOfTransition, Map<LVarRef, InstanceRef> vars, AnmlProblem pb) {
        List<VarRef> svParams = new LinkedList<>();
        for(LVarRef v : sv.jArgs()) {
            svParams.add(valueOf(v, vars, pb));
        }
        return new Fluent(sv.func(), svParams, valueOf(value, vars, pb), partOfTransition);
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
            if(aa.jDecompositions().size() == 0) {
                try {
                    actions.add(new GAction(aa, -1, params, gPb));
                } catch (FAPEException e) {}
            } else {
                for(int i=0 ; i<aa.jDecompositions().size() ; i++) {
                    try {
                        actions.add(new GAction(aa, i, params, gPb));
                    } catch (FAPEException e) {}
                }
            }
        }

        return actions;
    }

    public ArrayList<GTaskCond> getActionRefs() {
        return subTasks;
    }

    public ArrayList<GTaskCond> initSubTasks(AnmlProblem pb) {
        List<AbstractActionRef> refs;
        List<GTaskCond> ret = new LinkedList<>();
        if(decID == -1)
            refs = this.abs.jActions();
        else {
            refs = new LinkedList<>(this.abs.jActions());
            refs.addAll(this.abs.jDecompositions().get(decID).jActions());
        }
        for(AbstractActionRef ref : refs) {
            List<InstanceRef> args = new LinkedList<>();
            for(LVarRef v : ref.jArgs()) {
                args.add(valueOf(v));
            }
            ret.add(new GTaskCond(pb.getAction(ref.name()), args));
        }

        ArrayList<GTaskCond> subTasks = new ArrayList<>(ret.size());
        for(int i=0 ; i<ret.size() ; i++)
            subTasks.add(ret.get(i));

        return subTasks;
    }
}
