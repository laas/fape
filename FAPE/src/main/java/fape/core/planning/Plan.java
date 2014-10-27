package fape.core.planning;


import fape.core.planning.states.Printer;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.util.ActionsChart;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.constraints.stnu.DispatchableSTNU;
import planstack.constraints.stnu.STNUDispatcher;
import planstack.graph.core.Edge;
import planstack.graph.core.UnlabeledDigraph;
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList;
import planstack.graph.printers.NodeEdgePrinter;
import planstack.structures.IList;
import scala.collection.JavaConversions;

import java.util.*;

public class Plan {

    /** if set to true, planned actions will be displayed in a graphical window */
    public static boolean showChart = true;

    /** if true, building a Plan will create a dispatchable STNU */
    public static boolean makeDispatchable = false;

    class PlanPrinter extends NodeEdgePrinter<Action, Object, Edge<Action>> {
        @Override
        public String printNode(Action a) {
            return Printer.action(st, a);
        }
    }
    final State st;
    STNUDispatcher<TPRef, GlobalRef> dispatcher;
    final Map<TPRef, Action> actions = new HashMap<>();
    final UnlabeledDigraph<LogStatement> eventsDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();
    final UnlabeledDigraph<Action> actionDependencies = new SimpleUnlabeledDirectedAdjacencyList<>();

    public Plan(final State st) {
        assert st.isConsistent() : "Cannot build a plan from an inconsistent state.";
        this.st = st;

        if(makeDispatchable) {
            this.dispatcher = st.getDispatchableSTNU();

            for (Action a : st.getAllActions()) {
                if (a.status() == ActionStatus.EXECUTED || a.status() == ActionStatus.EXECUTING || a.status() == ActionStatus.FAILED)
                    dispatcher.setHappened(a.start());
                if (a.status() == ActionStatus.EXECUTED || a.status() == ActionStatus.FAILED)
                    dispatcher.setHappened(a.end());

                // record that this time point is the start of this action
                actions.put(a.start(), a);
            }

            // Uncomment for the debugging output of
            /*
            class Printer extends NodeEdgePrinter<TPRef, Object, Edge<TPRef>> {
                public String printNode(TPRef tp) {
                    return fape.core.planning.states.Printer.timepoint(st, tp);
                }
            }
            dispatcher.print(new Printer());
            */
        }
        if(showChart)
            showChart();
    }

    public void showChart() {
        List<Action> acts = new LinkedList<>(st.getAllActions());
        Collections.sort(acts, new Comparator<Action>() {
            @Override
            public int compare(Action a1, Action a2) {
                return (int) (st.getEarliestStartTime(a1.start()) - st.getEarliestStartTime(a2.start()));
            }
        });

        for(Action a : acts) {
            int start = (int) st.getEarliestStartTime(a.start());
            int earliestEnd = (int) st.getEarliestStartTime(a.end());
            String name = Printer.action(st, a);
            switch (a.status()) {
                case EXECUTED:
                    ActionsChart.addExecutedAction(name, start, earliestEnd);
                    break;
                case EXECUTING:
                case PENDING:
                    if(st.getDurationBounds(a).nonEmpty()) {
                        int min = st.getDurationBounds(a).get()._1();
                        int max = st.getDurationBounds(a).get()._2();
                        ActionsChart.addPendingAction(name, start, min, max);
                    } else {
                        ActionsChart.addPendingAction(name, start, earliestEnd - start, earliestEnd-start);
                    }
                    break;
                case FAILED:
                    ActionsChart.addFailedAction(name, start, earliestEnd);
                    break;
            }
        }
    }

    public State getState() { return st; }

    public boolean isConsistent() {
        return st.isConsistent() && (!makeDispatchable || dispatcher.isConsistent());
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

    public Collection<Action> getExecutableActions(int atTime) {
        assert makeDispatchable && dispatcher != null :
                "Cannot compute executable actions without the 'makeDispatchable' option turned on.";
        IList<TPRef> toDispatch = dispatcher.getDispatchable(atTime);
        List<Action> dispatchable = new LinkedList<>();
        for(TPRef tp : toDispatch) {
            assert actions.containsKey(tp) : "This time point does not seem to be an action start: "+tp;
            assert actions.get(tp).status() == ActionStatus.PENDING : "An action selected for execution is not pending.";
            dispatchable.add(actions.get(tp));
        }
        return dispatchable;
    }

    public int getMaxDuration(Action act) {
        assert makeDispatchable && dispatcher != null : "makeDispatchable option is turned off";
        return dispatcher.getMaxDelay(act.start(), act.end());
    }

    public int getMinDuration(Action act) {
        assert makeDispatchable && dispatcher != null : "makeDispatchable option is turned off";
        return dispatcher.getMinDelay(act.start(), act.end());
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
