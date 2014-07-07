package fape.core.planning.constraints;

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
public class ConservativeConstraintNetwork<VarRef> {

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
     * Contains all extension constraints defined in the CSP.
     * For any constraints involving two variables a and b, there needs to be an
     * edge (a, b, EXTENSION) in the constraint graph.
     */
    final HashMap<String, ExtensionConstraint<VarRef>> exts;
    HashMap<List<VarRef>, String> mappings;

    /**
     * All constraints that need to processed for incremental consistency checking.
     */
    final Queue<VarRef> toProcess;

    /**
     * Flag indicating if the CSP is consistent (e.g. no variables with empty domain).
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
        exts = new HashMap<String, ExtensionConstraint<VarRef>>();
        mappings = new HashMap<>();
    }

    public ConservativeConstraintNetwork(ConservativeConstraintNetwork base) {
        variables = base.variables;
        types = base.types; // todo, need to copy?
        values = base.values;
        valuesIds = base.valuesIds;
        constraints = base.constraints.cc();
        toProcess = new LinkedList<VarRef>(base.toProcess);
        consistent = base.consistent;
        mappings = new HashMap<List<VarRef>, String>(base.mappings);
        exts = base.exts;
    }

    public void domainModified(VarRef v) {
        if(!toProcess.contains(v))
            toProcess.add(v);

        List<List<VarRef>> constraintsToCheck = new LinkedList<>();
        if(domainOf(v).size() == 0) {
            consistent = false;
            return;
        } else if(domainOf(v).size() == 1) {
            for(List<VarRef> constraint : mappings.keySet()) {
                if(constraint.contains(v))
                    constraintsToCheck.add(constraint);
            }
        }

        for(List<VarRef> cons : constraintsToCheck) {
            for(int focus=0 ; focus<cons.size() ; focus++) {
                VarRef focusVar = cons.get(focus);
                Map<Integer, Set<Integer>> restrictions = new HashMap<>();
                for(int j=0 ; j<cons.size() ; j++) {
                    if(j != focus)
                        restrictions.put(j, variables.apply(cons.get(j)).values());
                }

                ValuesHolder old = variables.apply(focusVar);
                Set<Integer> domainRestrictions = exts.get(mappings.get(cons)).valuesUnderRestriction(focus, restrictions);
                variables = variables.updated(focusVar, variables.apply(focusVar).intersect(new ValuesHolder(domainRestrictions)));
                if(!old.equals(variables.apply(focusVar)))
                    domainModified(focusVar);
            }
        }
    }

    public void addPossibleValue(String val) {
        if(!valuesIds.containsKey(val)) {
            int id = values.size();
            values.add(id, val);
            valuesIds.put(val, id);
        }
    }

    public boolean isConsistent() {
        if(!consistent) {
            return false;
        }

        if(!toProcess.isEmpty()) {
            while(!toProcess.isEmpty()) {
                VarRef c = toProcess.remove();
                consistent &= revise(c);
            }
        } else {
            return consistent;
        }
        return consistent;
    }

    private ValuesHolder vals(VarRef var) {
        return variables.apply(var);
    }

    protected boolean revise(VarRef v) {
        boolean res = true;
        for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(v)))
            res &= revise(v, neighbour);

        return res;
    }

    protected boolean revise(VarRef v1, VarRef v2) {
        boolean isConsistent = true;
        for(LabeledEdge<VarRef, ConstraintType> c :JavaConversions.asJavaCollection(constraints.edges(v1, v2))) {
            if(c.l() == ConstraintType.DIFFERENCE) {
                isConsistent &= reviseDifference(c);
            } else if(c.l() == ConstraintType.EQUALITY) {
                isConsistent &= reviseEquality(c);
            } else {
                throw new RuntimeException("Unknown constraint type: "+ c);
            }
        }
        return isConsistent;
    }

    protected boolean restrictDomain(VarRef v, ValuesHolder values) {
        ValuesHolder old = variables.apply(v);
        ValuesHolder newDom = old.intersect(values);
        variables = variables.updated(v, newDom);

        if(newDom.isEmpty()) {
            consistent = false;
            return true;
        } else if(!old.equals(newDom)) {
            domainModified(v);
            return true;
        } else {
            return false;
        }
    }

    protected boolean reviseDifference(LabeledEdge<VarRef, ConstraintType> c) {
        assert c.l() == ConstraintType.DIFFERENCE;

        if(variables.apply(c.u()).size() == 1) {
            if(vals(c.v()).contains((Integer) vals(c.u()).values.head())) {
                boolean changed = restrictDomain(c.v(), vals(c.v()).remove(vals(c.u())));
                assert changed;
            }
        }

        if(variables.apply(c.v()).size() == 1) {
            if(vals(c.u()).contains((Integer) vals(c.v()).values.head())) {
                boolean changed = restrictDomain(c.u(), vals(c.u()).remove(vals(c.v())));
                assert changed;
            }
        }

        return !vals(c.u()).isEmpty() && !vals(c.v()).isEmpty();
    }

    protected boolean reviseEquality(LabeledEdge<VarRef, ConstraintType> c) {
        assert c.l() == ConstraintType.EQUALITY;

        if(!variables.apply(c.u()).equals(variables.apply(c.v()))) {
            ValuesHolder newDomain = variables.apply(c.u()).intersect(variables.apply(c.v()));
            restrictDomain(c.u(), variables.apply(c.v()));
            restrictDomain(c.v(), variables.apply(c.u()));
        }
        return consistent;
    }

    public boolean restrictDomain(VarRef var, Collection<String> toValues) {
        assert variables.contains(var);
        List<Integer> ids = new LinkedList<>();
        for(String value : toValues) {
            ids.add(valuesIds.get(value));
        }
        restrictDomain(var, new ValuesHolder(ids));

        return isConsistent();
    }

    public void AddVariable(VarRef var, Collection<String> domain, String type) {
        assert !variables.contains(var);
        List<Integer> valueIds = new LinkedList<>();
        for(String val : domain) {
            assert valuesIds.containsKey(val) : "Error: "+val+" is not known to the constraint network.";
            valueIds.add(valuesIds.get(val));
        }
        variables = variables.updated(var, new ValuesHolder(valueIds));
        domainModified(var);
        constraints.addVertex(var);
        types.put(var, type);
    }

    public void AddUnificationConstraint(VarRef a, VarRef b) {
        LabeledEdge<VarRef, ConstraintType> constraint = new LabeledEdge<VarRef, ConstraintType>(a, b, ConstraintType.EQUALITY);
        constraints.addEdge(a, b, ConstraintType.EQUALITY);
        toProcess.add(a);
        toProcess.add(b);
        isConsistent();
    }

    public void AddSeparationConstraint(VarRef a, VarRef b) {
        LabeledEdge<VarRef, ConstraintType> constraint = new LabeledEdge<VarRef, ConstraintType>(a, b, ConstraintType.DIFFERENCE);
        constraints.addEdge(a, b, ConstraintType.DIFFERENCE);
        toProcess.add(a);
        toProcess.add(b);
        isConsistent();
    }

    public boolean contains(VarRef v) {
        return variables.contains(v);
    }

    public Collection<String> domainOf(VarRef var) {
        List<String> domain = new LinkedList<>();
        for(Integer id : variables.apply(var).values()) {
            domain.add(values.get(id));
        }
        return domain;
    }

    public String typeOf(VarRef v) {
        assert types.containsKey(v);
        return types.get(v);
    }

    public ConservativeConstraintNetwork<VarRef> DeepCopy() {
        return new ConservativeConstraintNetwork<>(this);
    }

    public boolean unifiable(VarRef a, VarRef b) {
        return !separated(a,b) && variables.apply(a).intersect(variables.apply(b)).values.size() != 0;
    }

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

    public boolean unified(VarRef a, VarRef b) {
        for(LabeledEdge<VarRef, ConstraintType> c : JavaConversions.asJavaCollection(constraints.edges(a, b))) {
            if(c.l() == ConstraintType.EQUALITY)
                return true;
        }
        return false;
    }

    public boolean separated(VarRef a, VarRef b) {
        for(LabeledEdge<VarRef, ConstraintType> c : JavaConversions.asJavaCollection(constraints.edges(a, b))) {
            if(c.l() == ConstraintType.DIFFERENCE)
                return true;
        }
        return false;
    }

    public List<VarRef> getUnboundVariables() {
        List<VarRef> unbound = new LinkedList<>();
        for(Tuple2<VarRef, ValuesHolder> varValues : JavaConversions.asJavaList(variables.toList())) {
            if(varValues._2().size() > 1)
                unbound.add(varValues._1());
        }
        return unbound;
    }

    public void addValuesToValuesSet(String setID, List<String> values) {
        List<Integer> vals = new LinkedList<>();
        for(String v : values) {
            assert valuesIds.containsKey(v);
            vals.add(valuesIds.get(v));
        }
        if(!exts.containsKey(setID)) {
            exts.put(setID, new ExtensionConstraint<VarRef>());
        }

        exts.get(setID).addValues(vals);
    }

    public void addValuesSetConstraint(List<VarRef> variables, String setID) {
        assert exts.containsKey(setID);
        assert !exts.get(setID).values.isEmpty();
        assert exts.get(setID).values.get(0).size() == variables.size();
        mappings.put(new LinkedList<VarRef>(variables), setID);
        for(VarRef v : variables) {
            domainModified(v);
        }
    }

    public String Report() {
        StringBuilder builder = new StringBuilder();
        for(VarRef var : JavaConversions.asJavaCollection(variables.keys())) {
            builder.append(var);
            builder.append("  ");
            builder.append(domainOf(var));
            builder.append("\n");
        }
        builder.append("\n");
        for(LabeledEdge<VarRef,ConstraintType> c : constraints.jEdges()) {
            builder.append(c.u()+":");
            builder.append(domainOf(c.u()));
            if(c.l() == ConstraintType.DIFFERENCE)
                builder.append(" != ");
            else if(c.l() == ConstraintType.EQUALITY)
                builder.append(" == ");
            builder.append(c.v()+":");
            builder.append(domainOf(c.v()));
            builder.append("\n");
        }
        builder.append("\n----------------\n");
        return builder.toString();
    }

}
