package fape.core.planning.preprocessing;

import fape.core.planning.planninggraph.PGUtils;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.exceptions.FAPEException;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.Function;
import planstack.anml.model.LVarRef;
import planstack.anml.model.SymFunction;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractDecomposition;
import planstack.anml.model.abs.statements.AbstractAssignment;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.abs.statements.AbstractPersistence;
import planstack.anml.model.abs.statements.AbstractTransition;
import planstack.anml.model.concrete.Decomposition;
import planstack.anml.model.concrete.VarRef;
import planstack.graph.GraphFactory;
import planstack.graph.core.LabeledEdge;
import planstack.graph.core.MultiLabeledDigraph;
import scala.collection.JavaConversions;

import java.util.*;

public class LiftedDTG implements ActionSupporterFinder{

    final AnmlProblem problem;

    MultiLabeledDigraph<FluentType, SupportingAction> dag = GraphFactory.getMultiLabeledDigraph();


    public LiftedDTG(AnmlProblem pb) {
        this.problem = pb;

        for(Function func : pb.functions().getAll()) {
            if(func instanceof SymFunction) {
                FluentType fluent = new FluentType(func.name(), JavaConversions.asJavaList(func.argTypes()), func.valueType());
                for(FluentType derived : derivedSubTypes(fluent)) {
                    if(!dag.contains(derived))
                        dag.addVertex(derived);
                }
            }
        }

        // build the constraint between fluents types
        for(AbstractAction aa : problem.abstractActions()) {
            for(AbstractLogStatement s : aa.jLogStatements()) {
                if(s instanceof AbstractTransition || s instanceof AbstractAssignment) {
                    for(FluentType prec : getPreconditions(aa, s)) {
                        for(FluentType eff : getEffects(aa, s)) {
                            if(!dag.contains(prec))
                                dag.addVertex(prec);
                            if(!dag.contains(eff))
                                dag.addVertex(eff);
                            dag.addEdge(prec, eff, new SupportingAction(aa));
                        }
                    }
                }
            }
            for(int decID=0 ; decID < aa.jDecompositions().size() ; decID++) {
                AbstractDecomposition dec = aa.jDecompositions().get(decID);
                for (AbstractLogStatement s : dec.jLogStatements()) {
                    if (s instanceof AbstractTransition || s instanceof AbstractAssignment) {
                        for (FluentType prec : getPreconditions(aa, s)) {
                            for (FluentType eff : getEffects(aa, s)) {
                                if (!dag.contains(prec))
                                    dag.addVertex(prec);
                                if (!dag.contains(eff))
                                    dag.addVertex(eff);
                                dag.addEdge(prec, eff, new SupportingAction(aa, decID));
                            }
                        }
                    }
                }
            }
        }
//        dag.exportToDotFile("dtg.dot");
    }

    public Collection<SupportingAction> getActionsSupporting(State st, TemporalDatabase db) {
        assert db.isConsumer() : "Error: this database doesn't need support: "+db;

        String predicate = db.stateVariable.func().name();
        List<String> argTypes = new LinkedList<>();
        for(VarRef argVar : db.stateVariable.jArgs()) {
            argTypes.add(st.typeOf(argVar));
        }
        String valueType = st.typeOf(db.GetGlobalConsumeValue());
        return this.getActionsSupporting(new FluentType(predicate, argTypes, valueType));

    }

    public Set<SupportingAction> getActionsSupporting(FluentType f) {
        Set<SupportingAction> supporters = new HashSet<>();

        try {
            for(LabeledEdge<FluentType, SupportingAction> inEdge : JavaConversions.asJavaList(dag.inEdges(f))) {
                supporters.add(inEdge.l());
            }
        } catch (NoSuchElementException e) {
            // type is not recorded
            e.printStackTrace();
            System.err.println("Unable to find a type: "+f.toString());
            System.err.println("Possible signatures are: ");
            for(FluentType ft : JavaConversions.asJavaIterable(dag.vertices()))
                System.err.println("   "+ft.toString());
            throw new FAPEException("Unable to find type: "+f+". See error output.");
        }
        return supporters;
    }

    public Set<FluentType> getEffects(AbstractAction a, AbstractLogStatement s) {
        Set<FluentType> allEffects = new HashSet<>();
        List<String> argTypes = new LinkedList<>();
        String valType = null;
        if(s instanceof AbstractTransition) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractTransition) s).to());

        } else if(s instanceof AbstractAssignment) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractAssignment) s).value());
        } else {
            // this statement has no effects
            return allEffects;
        }

        FluentType fluent = new FluentType(s.sv().func().name(), argTypes, valType);
        allEffects.addAll(derivedSubTypes(fluent));
        return allEffects;
    }

    public Set<FluentType> getEffects(AbstractAction a) {
        Set<FluentType> allEffects = new HashSet<>();
        for(AbstractLogStatement s : a.jLogStatements()) {
            allEffects.addAll(getEffects(a, (AbstractLogStatement) s));
        }
        return allEffects;
    }

    public Set<FluentType> getPreconditions(AbstractAction a, AbstractLogStatement s) {
        Set<FluentType> allPrecond = new HashSet<>();
        List<String> argTypes = new LinkedList<>();
        String valType = null;
        if(s instanceof AbstractTransition) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractTransition) s).from());

        } else if(s instanceof AbstractPersistence) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractPersistence) s).value());
        } else if(s instanceof AbstractAssignment) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            // the value before an assignment can be anything with the type of the state variable.
            valType = s.sv().func().valueType();
        } else {
            // this statement has no effects
            return allPrecond;
        }

        FluentType fluent = new FluentType(s.sv().func().name(), argTypes, valType);
        allPrecond.addAll(derivedSubTypes(fluent));
        return allPrecond;
    }

    public Set<FluentType> getPreconditions(AbstractAction a) {
        Set<FluentType> allPrecond = new HashSet<>();

        for(AbstractLogStatement s : a.jLogStatements()) {
            allPrecond.addAll(getPreconditions(a, s));
        }

        return allPrecond;
    }

    /**
     * Given a fluent type ft, returns all fluent types where the arg and value
     * types are subclasses of those of ft.
     *
     * This set includes ft itself.
     * @param ft
     * @return
     */
    public Set<FluentType> derivedSubTypes(FluentType ft) {
        Set<FluentType> allFluents = new HashSet<>();
        if(ft == null) {
            return allFluents;
        } else {
            List<List<String>> argTypesSets = new LinkedList<>();
            for(String argType : ft.argTypes) {
                argTypesSets.add(new LinkedList<>(problem.instances().subTypes(argType)));
            }

            for(List<String> argTypeList : PGUtils.allCombinations(argTypesSets)) {
                for(String valueType : problem.instances().subTypes(ft.valueType)) {
                    allFluents.add(new FluentType(ft.predicateName, argTypeList, valueType));
                }
            }
        }
        return allFluents;
    }
}
