package fape.core.planning;


import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionStatus;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.graph.core.Edge;
import planstack.graph.core.UnlabeledDigraph;
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList;
import scala.collection.JavaConversions;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Plan {

    final UnlabeledDigraph<LogStatement> eventsDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();
    final UnlabeledDigraph<Action> actionDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();

    public Plan(State st) {
        for(TemporalDatabase db : st.tdb.vars) {
            buildDependencies(db);
        }

        for(Edge<LogStatement> e :eventsDependencies.jEdges()) {
            Action a1 = st.getActionContaining(e.u());
            Action a2 = st.getActionContaining(e.v());

            if(a1 != null && a2 != null) {
                if(!actionDependencies.contains(a1))
                    actionDependencies.addVertex(a1);
                if(!actionDependencies.contains(a2))
                    actionDependencies.addVertex(a2);
                actionDependencies.addEdge(a1, a2);
            }
        }
        actionDependencies.exportToDotFile("plan.dot");
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
        for(int i=0 ; i<db.chain.size() ; i++) {
            for(int j=i+1 ; j<db.chain.size() ; j++) {
                addDependency(db.GetChainComponent(i), db.GetChainComponent(j));
            }
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

}
