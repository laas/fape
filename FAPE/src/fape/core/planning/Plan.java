package fape.core.planning;

import fape.core.planning.model.Action;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.events.TemporalEvent;
import planstack.graph.core.Edge;
import planstack.graph.core.SimpleUnlabeledDigraph;
import planstack.graph.core.UnlabeledDigraph;
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList;
import planstack.graph.printers.GraphDotPrinter;
import scala.collection.JavaConversions;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class Plan {

    public UnlabeledDigraph<TemporalEvent> eventsDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();
    public UnlabeledDigraph<Action> actionDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();

    public Plan(State st) {
        for(TemporalDatabase db : st.tdb.vars) {
            buildDependencies(db);
        }

        for(Edge<TemporalEvent> e :JavaConversions.asJavaCollection(eventsDependencies.edges())) {
            Action a1 = st.taskNet.getActionContainingEvent(e.u());
            Action a2 = st.taskNet.getActionContainingEvent(e.v());

            if(a1 != null && a2 != null) {
                if(!actionDependencies.contains(a1))
                    actionDependencies.addVertex(a1);
                if(!actionDependencies.contains(a2))
                    actionDependencies.addVertex(a2);
                actionDependencies.addEdge(a1, a2);
            }
        }

        GraphDotPrinter printer = new GraphDotPrinter(actionDependencies);
        printer.print2Dot("/home/abitmonn/tmp/g.dot");
    }

    public void addDependency(TemporalEvent e1, TemporalEvent e2) {
        if(!eventsDependencies.contains(e1))
            eventsDependencies.addVertex(e1);
        if(!eventsDependencies.contains(e2))
            eventsDependencies.addVertex(e2);

        eventsDependencies.addEdge(e1, e2);
    }

    public void addDependency(TemporalDatabase.ChainComponent c1, TemporalDatabase.ChainComponent c2) {
        for(TemporalEvent e1 : c1.contents) {
            for(TemporalEvent e2 : c2.contents) {
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
        for(Action a : JavaConversions.asJavaCollection(actionDependencies.vertices())) {
            boolean executable = true;
            if(a.status != Action.Status.PENDING) {
                executable = false;
                continue;
            }
            for(Action parent : JavaConversions.asJavaCollection(actionDependencies.parents(a))) {
                if(parent.status != Action.Status.EXECUTED) {
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
