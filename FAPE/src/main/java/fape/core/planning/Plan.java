package fape.core.planning;


import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionStatus;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.graph.core.Edge;
import planstack.graph.core.UnlabeledDigraph;
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList;
import planstack.graph.printers.NodeEdgePrinter;
import scala.collection.JavaConversions;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Plan {

    class PlanPrinter extends NodeEdgePrinter<Action, Object, Edge<Action>> {
        @Override
        public String printNode(Action a) {
            return Printer.action(st, a);
        }
    }
    final State st;
    final UnlabeledDigraph<LogStatement> eventsDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();
    final UnlabeledDigraph<Action> actionDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();

    public Plan(State st) {
        this.st = st;

        for(TemporalDatabase db : st.tdb.vars) {
            buildDependencies(db);
        }

        for(Edge<LogStatement> e :eventsDependencies.jEdges()) {
            addActionDependency(e.u(), e.v());
        }
    }

    /**
     * Adds a constraint between the actions containing the given statements.
     * The two statements have to appear in the event dependencies graph.
     * @param s1 Statement in eventDependencies
     * @param s2 Statement in eventDependencies
     */
    public void addActionDependency(LogStatement s1, LogStatement s2) {
        Action a1 = st.getActionContaining(s1);
        Action a2 = st.getActionContaining(s2);

        if(a1 != null && a2 != null) {
            if(!actionDependencies.contains(a1))
                actionDependencies.addVertex(a1);
            if(!actionDependencies.contains(a2))
                actionDependencies.addVertex(a2);
        }

        if(a1 == null) {
            //Do nothing
        } else if(a2 != null) {
            // add a dependency from a1 to a2
            actionDependencies.addEdge(a1, a2);
        } else { // a2 == null
            // skip s2 and add constraints between s1 and every child of s2
            for(LogStatement s2child : eventsDependencies.jChildren(s2)) {
                addActionDependency(s1, s2child);
            }
        }
    }

    public void addDependency(LogStatement e1, LogStatement e2) {
        if(!eventsDependencies.contains(e1))
            eventsDependencies.addVertex(e1);
        if(!eventsDependencies.contains(e2))
            eventsDependencies.addVertex(e2);

        eventsDependencies.addEdge(e1, e2);
    }

    public void addDependency(ChainComponent c1, ChainComponent c2) {
        for(LogStatement e1 : c1.contents) {
            for(LogStatement e2 : c2.contents) {
                addDependency(e1, e2);
            }
        }
    }

    public void buildDependencies(TemporalDatabase db) {
        for(int i=0 ; i<db.chain.size()-1 ; i++) {
            addDependency(db.GetChainComponent(i), db.GetChainComponent(i+1));
        }
    }


    public Collection<Action> GetNextActions() {
        List<Action> executableActions = new LinkedList<>();
        for(Action a : actionDependencies.jVertices()) {
            boolean executable = true;
            if(a.status() != ActionStatus.PENDING) {
                executable = false;
                continue;
            }
            for(Action pred : JavaConversions.asJavaCollection(actionDependencies.parents(a))) {
                if(pred.status() != ActionStatus.EXECUTED) {
                    executable = false;
                    break;
                }
            }
            if(executable) {
                executableActions.add(a);
            }
        }
        return executableActions;
    }

    public boolean completelyExecuted() {
        //for(Action a : JavaConversions.asJavaCollection(actionDependencies.vertices())) {
        return true;
    }

    /**
     * Write a representation of the plan to the dot format.
     * To compile <code>dot plan.dot -Tps > plan.ps</code>
     * @param fileName File to which the output will be written.
     */
    public void exportToDot(String fileName) {
        actionDependencies.exportToDotFile(fileName, new PlanPrinter());
    }
}
