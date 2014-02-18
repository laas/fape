package fape.core.planning.search.abstractions;

import fape.core.planning.Planner;
import fape.core.planning.model.AbstractAction;
import fape.core.planning.model.AbstractTemporalEvent;
import fape.core.planning.model.Type;
import fape.core.planning.temporaldatabases.events.propositional.PersistenceEvent;
import fape.core.planning.temporaldatabases.events.propositional.TransitionEvent;
import fape.exceptions.FAPEException;
import planstack.graph.core.DirectedGraph;
import planstack.graph.core.SimpleUnlabeledDigraph;
import planstack.graph.core.UnlabeledDigraph;
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList;
import planstack.graph.algorithms.StronglyConnectedComponent;
import scala.collection.immutable.Seq;

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

    final Planner planner;

    /**
     * Maps every fluent type to its group.
     */
    final HashMap<FluentType, Integer> fluentsGroup = new HashMap<>();

    private UnlabeledDigraph<FluentType> dag = new SimpleUnlabeledDirectedAdjacencyList<FluentType>();

    public AbstractionHierarchy(Planner planner) { //TODO: should be problem, not planner
        this.planner = planner;

        // build the constraint between fluents types
        for(AbstractAction aa : planner.actions.values()) {
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

    public int getLevel(String predicate, String argType, String valueType) {
        FluentType ft = new FluentType(predicate, argType, valueType);
        if(!fluentsGroup.containsKey(ft))
            throw new FAPEException("No recorded level for: " + ft);
        else
            return fluentsGroup.get(ft);
    }


    /**
     * Returns the type of a literal (parameter or object constant) appearing in
     * an abstract action.
     * @param a
     * @param var
     * @return
     */
    public String typeOf(AbstractAction a, String var) {
        String ret;
        try {
            ret = a.typeOfParameter(var);
        } catch (Exception e) {
            ret = planner.types.getObjectType(var);
        }
        return ret;
    }

    public Set<FluentType> getEffects(AbstractAction a) {
        Set<FluentType> allEffects = new HashSet<>();
        for(AbstractTemporalEvent e : a.events) {
            if(e.isTransitionEvent()) {
                allEffects.addAll(getEffects(a, e));
            }
        }
        return allEffects;
    }

    public Set<FluentType> getPreconditions(AbstractAction a) {
        Set<FluentType> allPrecond = new HashSet<>();
        for(AbstractTemporalEvent e : a.events) {
            allPrecond.addAll(getPreconditions(a, e));
        }
        return allPrecond;
    }

    public Set<FluentType> getPreconditions(AbstractAction a, AbstractTemporalEvent e) {
        FluentType fluent = null;
        if(e.isTransitionEvent()) {
            TransitionEvent te = (TransitionEvent) e.event;
            if(!te.from.isNull()) {
                String predicateName = e.stateVariableReference.refs.get(1);
                String argType = typeOf(a, e.stateVariableReference.GetConstantReference());
                String valueType = typeOf(a, te.from.var);
                fluent = new FluentType(predicateName, argType, valueType);
            }
        } else if(e.isPersistenceEvent()) {
            PersistenceEvent te = (PersistenceEvent) e.event;
            if(!te.value.isNull()) {
                String predicateName = e.stateVariableReference.refs.get(1);
                String argType = typeOf(a, e.stateVariableReference.GetConstantReference());
                String valueType = typeOf(a, te.value.var);
                fluent = new FluentType(predicateName, argType, valueType);
            }
        } else {
            throw new FAPEException("Unsupported event type: "+e);
        }

        return derivedSubTypes(fluent);
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
            for(Type argSubType : planner.types.subtypes(ft.argType)) {
                for(Type valueSubType : planner.types.subtypes(ft.valueType)) {
                    allFluents.add(new FluentType(ft.predicateName, argSubType.name, valueSubType.name));
                }
            }
        }
        return allFluents;
    }

    public Set<FluentType> getEffects(AbstractAction a, AbstractTemporalEvent e) {
        TransitionEvent te = (TransitionEvent) e.event;
        String predicateName = e.stateVariableReference.refs.get(1);
        String argType = typeOf(a, e.stateVariableReference.GetConstantReference());
        String valueType = typeOf(a, te.to.var);

        return derivedSubTypes(new FluentType(predicateName, argType, valueType));
    }
}
