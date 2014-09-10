package fape.core.planning.preprocessing;

import fape.core.planning.planninggraph.PGUtils;
import fape.exceptions.FAPEException;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.*;
import planstack.graph.GraphFactory;
import planstack.graph.algorithms.StronglyConnectedComponent;
import planstack.graph.core.UnlabeledDigraph;

import java.util.*;


/**
* A lifted abstraction hierarchy.
* It separates fluents in different levels based on their types.
*
* Separation is done using a slight adaptation of the problem independent algorithm of
* Knoblock 94: Automatically Generating Abstractions for Planning
* Adaptation focuses staying lifted and handling type inheritance.
*/
public class AbstractionHierarchy {

    final AnmlProblem problem;

    /**
     * Maps every fluent type to its group.
     */
    final HashMap<FluentType, Integer> fluentsGroup = new HashMap<>();

    private UnlabeledDigraph<FluentType> dag = GraphFactory.getSimpleUnlabeledDigraph();

    public AbstractionHierarchy(AnmlProblem pb) {
        this.problem = pb;

        // build the constraint between fluents types
        for(AbstractAction aa : problem.abstractActions()) {
            Set<FluentType> effects = getEffects(aa);
            Set<FluentType> preconditions = getPreconditions(aa);
            // given an effect t1 of action a
            for(FluentType ft1 : effects) {
                if(!dag.contains(ft1))
                    dag.addVertex(ft1);

                // for any effect ft2 of the same action
                for(FluentType ft2 : effects) {
                    if(!dag.contains(ft2))
                        dag.addVertex(ft2);

                    // add constraints ft1 -> ft2 and ft2 -> ft1
                    dag.addEdge(ft1, ft2);
                    dag.addEdge(ft2, ft1);
                }
                // for any precondition of the same action
                for(FluentType ft2 : preconditions) {
                    if(!dag.contains(ft2))
                        dag.addVertex(ft2);

                    // add constraints ft1 -> ft2
                    dag.addEdge(ft1, ft2);
                }
            }
        }

        // Get the strongly connected component of the constraint graph
        StronglyConnectedComponent scc = new StronglyConnectedComponent(dag);

        // topological sort of the strongly connected components gives us the final hierarchy
        List<Set<FluentType>> groups = scc.jTopologicalSortOfReducedGraph();
        for(int level=0 ; level<groups.size() ; level++) {
            Set<FluentType> group = groups.get(level);
            for(FluentType ft : group) {
                fluentsGroup.put(ft, level);
            }
        }
    }

    public int getLevel(String predicate, List<String> argTypes, String valueType) {
        FluentType ft = new FluentType(predicate, argTypes, valueType);
        if(!fluentsGroup.containsKey(ft))
            throw new FAPEException("No recorded level for: " + ft);
        else
            return fluentsGroup.get(ft);
    }

    public Set<FluentType> getEffects(AbstractAction a) {
        Set<FluentType> allEffects = new HashSet<>();
        for(AbstractLogStatement ls : a.jLogStatements()) {
            List<String> argTypes = new LinkedList<>();
            String valType = null;
            if (ls instanceof AbstractTransition) {
                for (LVarRef arg : ls.sv().jArgs()) {
                    argTypes.add(a.context().getType(arg));
                }
                valType = a.context().getType(((AbstractTransition) ls).to());

            } else if (ls instanceof AbstractAssignment) {
                for (LVarRef arg : ls.sv().jArgs()) {
                    argTypes.add(a.context().getType(arg));
                }
                valType = a.context().getType(((AbstractAssignment) ls).value());
            } else {
                // this statement has no effects
                continue;
            }

            FluentType fluent = new FluentType(ls.sv().func().name(), argTypes, valType);
            allEffects.addAll(derivedSubTypes(fluent));
        }
        return allEffects;
    }

    public Set<FluentType> getPreconditions(AbstractAction a) {
        Set<FluentType> allPrecond = new HashSet<>();
        for(AbstractLogStatement s : a.jLogStatements()) {
            List<String> argTypes = new LinkedList<>();
            String valType = null;
            if (s instanceof AbstractTransition) {
                for (LVarRef arg : s.sv().jArgs()) {
                    argTypes.add(a.context().getType(arg));
                }
                valType = a.context().getType(((AbstractTransition) s).from());

            } else if (s instanceof AbstractPersistence) {
                for (LVarRef arg : s.sv().jArgs()) {
                    argTypes.add(a.context().getType(arg));
                }
                valType = a.context().getType(((AbstractPersistence) s).value());
            } else {
                // this statement has no effects
                continue;
            }

            FluentType fluent = new FluentType(s.sv().func().name(), argTypes, valType);
            allPrecond.addAll(derivedSubTypes(fluent));
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

    public void exportToDot(String fileName) {
        dag.exportToDotFile(fileName);
    }
}
