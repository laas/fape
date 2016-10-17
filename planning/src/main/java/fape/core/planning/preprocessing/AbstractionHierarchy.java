package fape.core.planning.preprocessing;

import fape.util.TinyLogger;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.Function;
import planstack.anml.model.LVarRef;
import planstack.anml.model.Type;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.AbstractAssignment;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.abs.statements.AbstractPersistence;
import planstack.anml.model.abs.statements.AbstractTransition;
import planstack.graph.GraphFactory;
import planstack.graph.algorithms.StronglyConnectedComponent;
import planstack.graph.core.UnlabeledDigraph;

import java.util.*;
import java.util.stream.Collectors;


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
    final HashMap<Function, Integer> fluentsGroup = new HashMap<>();

    private UnlabeledDigraph<Function> dag = GraphFactory.getSimpleUnlabeledDigraph();

    public AbstractionHierarchy(AnmlProblem pb) {
        this.problem = pb;

        // build the constraint between fluents types
        for(AbstractAction aa : problem.abstractActions()) {
            Set<Function> effects = getEffects(aa);
            Set<Function> preconditions = getPreconditions(aa);
            // given an effect t1 of action a
            for(Function ft1 : effects) {
                if(!dag.contains(ft1))
                    dag.addVertex(ft1);

                // for any effect ft2 of the same action
                for(Function ft2 : effects) {
                    if(!dag.contains(ft2))
                        dag.addVertex(ft2);

                    // add constraints ft1 -> ft2 and ft2 -> ft1
                    dag.addEdge(ft1, ft2);
                    dag.addEdge(ft2, ft1);
                }
                // for any precondition of the same action
                for(Function ft2 : preconditions) {
                    if(!dag.contains(ft2))
                        dag.addVertex(ft2);

                    // add constraints ft1 -> ft2
                    dag.addEdge(ft1, ft2);
                }
            }
        }
        for(Function fn : pb.functions().getAll()) {
            if(!fn.isConstant() && !dag.contains(fn)) {
                dag.addVertex(fn);
            }
        }

        // Get the strongly connected component of the constraint graph
        StronglyConnectedComponent<Function> scc = new StronglyConnectedComponent<>(dag);

        // topological sort of the strongly connected components gives us the final hierarchy
        List<Set<Function>> groups = scc.jTopologicalSortOfReducedGraph();
        for(int level=0 ; level<groups.size() ; level++) {
            Set<Function> group = groups.get(level);
            for(Function ft : group) {
                fluentsGroup.put(ft, level);
//                System.out.println(level+"  "+ft);
            }
        }
    }

    /**
     * Return the layer of the abstraction hierarchy at chich the function is.
     * 0 is the top level.
     */
    public int getLevel(Function func) {
        if(fluentsGroup.isEmpty())
            return 0; // there seem to be no action model
        assert fluentsGroup.containsKey(func) : "State variable \""+func+"\" does not appear in the abstraction hierarchy";
        return fluentsGroup.get(func);
    }

    public Set<Function> getEffects(AbstractAction a) {
        Set<Function> allEffects = new HashSet<>();
        for(AbstractLogStatement ls : a.jLogStatements()) {
            if(ls.hasEffectAtEnd())
                allEffects.add(ls.sv().func());
        }
        return allEffects;
    }

    public Set<Function> getPreconditions(AbstractAction a) {
        Set<Function> allPrecond = new HashSet<>();
        for(AbstractLogStatement s : a.jLogStatements()) {
            if(s.hasConditionAtStart())
                allPrecond.add(s.sv().func());
        }
        return allPrecond;
    }

    public void exportToDot(String fileName) {
        dag.exportToDotFile(fileName);
    }
}
