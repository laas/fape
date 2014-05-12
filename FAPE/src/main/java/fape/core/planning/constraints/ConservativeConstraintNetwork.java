package fape.core.planning.constraints;

import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.VarRef;
import planstack.graph.core.Edge;
import planstack.graph.core.LabeledEdge;
import planstack.graph.core.impl.MultiLabeledUndirectedAdjacencyList;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.util.*;

/**
 * Implements a CSP with a very conservative strategy to allow sharing
 * most of internal data structures with related CSPs (e.g. CSP obtained by
 * cloning this one).
 *
 * It relies on scala immutable data structures to allow sharing with other
 * ConservativeConstraintNetworks.
 *
 * Internal representation of the domains maps every domain value to an integer,
 * and uses a BitSet to represent a set of values.
 * Domain holders objects are immutable as well and as such can be shared between
 * multiple instances. (See the ValuesHolder class).
 */
public class ConservativeConstraintNetwork extends ConstraintNetwork {

    enum ConstraintType { EQUALITY, DIFFERENCE }

    /**
     * Maps variables to their domain. The ValuesHolder object is to be recreated whenever a
     * modification is done.
     */
    scala.collection.immutable.Map<VarRef, ValuesHolder> variables;

    /**
     * Maps variables to their type. This instance is shared with all descendants.
     * Hence type CANNOT be updated without updating all the the other CSPs.
     */
    final HashMap<VarRef, String> types;

    /**
     * All values that can be part of a domain.
     * Its is used as a map from an ID to a value.
     *
     * It is shared between all instances.
     */
    final ArrayList<String> values;

    /**
     * Maps every value to its ID.
     * Shared between all instances.
     */
    final HashMap<String, Integer> valuesIds;

    /**
     * A graph containing all the constraints of the CSP.
     * Nodes are variables and edges are labeled with a constraint type.
     */
    final MultiLabeledUndirectedAdjacencyList<VarRef, ConstraintType> constraints;

    /**
     * All constraints that need to processed for incremental consistency checking.
     */
    final Queue<LabeledEdge<VarRef, ConstraintType>> toProcess;

    /**
     * Flag indicating if the CSP is consistent (e.g. no varaibles with empty domain).
     * If toProcess is not empty, then a consistency check has to be performed before
     * checking this flag.
     * However if this flag is set to false, the CSP is guaranteed to be inconsistent even
     * if toProcess is non-empty.
     */
    boolean consistent;

    public ConservativeConstraintNetwork() {
        toProcess = new LinkedList<>();
        variables = new scala.collection.immutable.HashMap<>();
        types = new HashMap<>();
        values = new ArrayList<>();
        valuesIds = new HashMap<>();
        constraints = new MultiLabeledUndirectedAdjacencyList<>();
        consistent = true;
    }

    public ConservativeConstraintNetwork(ConservativeConstraintNetwork base) {
        variables = base.variables;
        types = base.types; // todo, need to copy?
        values = base.values;
        valuesIds = base.valuesIds;
        constraints = base.constraints.cc();
        toProcess = new LinkedList<>(base.toProcess);
        consistent = base.consistent;
    }

    public void addPossibleValue(String val) {
        if(!valuesIds.containsKey(val)) {
            int id = values.size();
            values.add(id, val);
            valuesIds.put(val, id);
        }
    }

    @Override
    public boolean isConsistent() {
        if(!consistent) {
            return false;
        }

        if(!toProcess.isEmpty()) {
            while(!toProcess.isEmpty()) {
                LabeledEdge<VarRef, ConstraintType> c = toProcess.remove();
                consistent = consistent && revise(c);
            }
        } else {
            return consistent;
        }
        return consistent;
    }

    private ValuesHolder vals(VarRef var) {
        return variables.apply(var);
    }

    protected boolean revise(LabeledEdge<VarRef, ConstraintType> c) {
        if(c.l() == ConstraintType.DIFFERENCE) {
            return reviseDifference(c);
        } else if(c.l() == ConstraintType.EQUALITY) {
            return reviseEquality(c);
        } else {
            throw new FAPEException("Unknwon constraint type: "+ c);
        }
    }

    protected boolean reviseDifference(LabeledEdge<VarRef, ConstraintType> c) {
        assert c.l() == ConstraintType.DIFFERENCE;

        if(variables.apply(c.u()).size() == 1) {
            if(vals(c.v()).contains((Integer) vals(c.u()).values.head())) {
                variables = variables.updated(c.v(), vals(c.v()).remove(vals(c.u())));
                toProcess.addAll(JavaConversions.asJavaCollection(constraints.edges(c.u())));
            }
        }

        if(variables.apply(c.v()).size() == 1) {
            if(vals(c.u()).contains((Integer) vals(c.v()).values.head())) {
                variables = variables.updated(c.u(), vals(c.u()).remove(vals(c.v())));
                toProcess.addAll(JavaConversions.asJavaCollection(constraints.edges(c.v())));
            }
        }

        return !vals(c.u()).isEmpty() && !vals(c.v()).isEmpty();
    }

    protected boolean reviseEquality(LabeledEdge<VarRef, ConstraintType> c) {
        assert c.l() == ConstraintType.EQUALITY;

        if(!variables.apply(c.u()).equals(variables.apply(c.v()))) {
            ValuesHolder newDomain = variables.apply(c.u()).intersect(variables.apply(c.v()));
            variables = variables.updated(c.u(), newDomain);
            variables = variables.updated(c.v(), newDomain);
            toProcess.addAll(JavaConversions.asJavaCollection(constraints.edges(c.u())));
            toProcess.addAll(JavaConversions.asJavaCollection(constraints.edges(c.v())));

            if(newDomain.values.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean restrictDomain(VarRef var, Collection<String> toValues) {
        assert variables.contains(var);
        List<Integer> ids = new LinkedList<>();
        for(String value : toValues) {
            ids.add(valuesIds.get(value));
        }
        variables = variables.updated(var, variables.apply(var).intersect(new ValuesHolder(ids)));

        toProcess.addAll(JavaConversions.asJavaCollection(constraints.edges(var)));

        return isConsistent();
    }

    @Override
    public void AddVariable(VarRef var, Collection<String> domain, String type) {
        assert !variables.contains(var);
        List<Integer> valueIds = new LinkedList<>();
        for(String val : domain) {
            assert valuesIds.containsKey(val) : "Error: "+val+" is not known to the constraint network.";
            valueIds.add(valuesIds.get(val));
        }
        variables = variables.updated(var, new ValuesHolder(valueIds));
        constraints.addVertex(var);
        types.put(var, type);
    }

    @Override
    public void AddUnificationConstraint(VarRef a, VarRef b) {
        LabeledEdge<VarRef, ConstraintType> constraint = new LabeledEdge<VarRef, ConstraintType>(a, b, ConstraintType.EQUALITY);
        constraints.addEdge(a, b, ConstraintType.EQUALITY);
        toProcess.add(constraint);
        isConsistent();
    }

    @Override
    public void AddSeparationConstraint(VarRef a, VarRef b) {
        LabeledEdge<VarRef, ConstraintType> constraint = new LabeledEdge<VarRef, ConstraintType>(a, b, ConstraintType.DIFFERENCE);
        constraints.addEdge(a, b, ConstraintType.DIFFERENCE);
        toProcess.add(constraint);
        isConsistent();
    }

    @Override
    public boolean contains(VarRef v) {
        return variables.contains(v);
    }

    @Override
    public Collection<String> domainOf(VarRef var) {
        List<String> domain = new LinkedList<>();
        for(Object v : JavaConversions.asJavaCollection(variables.apply(var).values)) {
            Integer id = (Integer) v;
            domain.add(values.get(id));
        }
        return domain;
    }

    @Override
    public String typeOf(VarRef v) {
        assert types.containsKey(v);
        return types.get(v);
    }

    @Override
    public ConstraintNetwork DeepCopy() {
        return new ConservativeConstraintNetwork(this);
    }

    @Override
    public boolean unifiable(VarRef a, VarRef b) {
        return !separated(a,b) && variables.apply(a).intersect(variables.apply(b)).values.size() != 0;
    }

    @Override
    public boolean separable(VarRef a, VarRef b) {
        if(unified(a, b))
            return false;
        else if(separated(a, b))
            return true;
        else if(vals(a).size() == 1 && vals(b).size() == 1)
            return !vals(a).equals(vals(b));
        else
            return true;
    }

    @Override
    public boolean unified(VarRef a, VarRef b) {
        for(LabeledEdge<VarRef, ConstraintType> c : JavaConversions.asJavaCollection(constraints.edges(a, b))) {
            if(c.l() == ConstraintType.EQUALITY)
                return true;
        }
        return false;
    }

    @Override
    public boolean separated(VarRef a, VarRef b) {
        for(LabeledEdge<VarRef, ConstraintType> c : JavaConversions.asJavaCollection(constraints.edges(a, b))) {
            if(c.l() == ConstraintType.DIFFERENCE)
                return true;
        }
        return false;
    }

    @Override
    public List<VarRef> getUnboundVariables() {
        List<VarRef> unbound = new LinkedList<>();
        for(Tuple2<VarRef, ValuesHolder> varValues : JavaConversions.asJavaList(variables.toList())) {
            if(varValues._2().size() > 1)
                unbound.add(varValues._1());
        }
        return unbound;
    }

    @Override
    public String Report() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        for(Edge<VarRef> c : constraints.jEdges()) {
            builder.append(c.u()+":");
            builder.append(domainOf(c.u()));
            builder.append(" == ");
            builder.append(c.v()+":");
            builder.append(domainOf(c.v()));
            builder.append("\n");
        }
        builder.append("\n----------------\n");
        return builder.toString();
    }

}
