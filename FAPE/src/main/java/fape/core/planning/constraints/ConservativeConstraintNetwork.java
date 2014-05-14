package fape.core.planning.constraints;

import fape.exceptions.FAPEException;
import fape.util.Pair;
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

    enum ConstraintType { EQUALITY, DIFFERENCE, EXTENSION }

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
    final LinkedList<ExtensionConstraint> exts = new LinkedList<>();

    /**
     * All constraints that need to processed for incremental consistency checking.
     */
    final Queue<Pair<VarRef, VarRef>> toProcess;

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
    }

    public ConservativeConstraintNetwork(ConservativeConstraintNetwork base) {
        variables = base.variables;
        types = base.types; // todo, need to copy?
        values = base.values;
        valuesIds = base.valuesIds;
        constraints = base.constraints.cc();
        toProcess = new LinkedList<>(base.toProcess);
        consistent = base.consistent;
        for(ExtensionConstraint ext : base.exts) {
            exts.add(ext.DeepCopy());
        }
    }

    @Override
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
                Pair<VarRef, VarRef> c = toProcess.remove();
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

    protected boolean revise(Pair<VarRef,VarRef> pair) {
        boolean isConsistent = true;
        for(LabeledEdge<VarRef, ConstraintType> c :JavaConversions.asJavaCollection(constraints.edges(pair.value1, pair.value2))) {
            if(c.l() == ConstraintType.DIFFERENCE) {
                isConsistent &= reviseDifference(c);
            } else if(c.l() == ConstraintType.EQUALITY) {
                isConsistent &= reviseEquality(c);
            } else if(c.l() == ConstraintType.EXTENSION) {
                isConsistent &= reviseExts(c.u(), c.v());
                isConsistent &= reviseExts(c.v(), c.u());
            } else {
                throw new FAPEException("Unknwon constraint type: "+ c);
            }
        }
        return isConsistent;
    }

    protected boolean reviseDifference(LabeledEdge<VarRef, ConstraintType> c) {
        assert c.l() == ConstraintType.DIFFERENCE;

        if(variables.apply(c.u()).size() == 1) {
            if(vals(c.v()).contains((Integer) vals(c.u()).values.head())) {
                variables = variables.updated(c.v(), vals(c.v()).remove(vals(c.u())));
                for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(c.v())))
                    if(neighbour != c.u())
                        toProcess.add(new Pair<>(neighbour, c.v()));
            }
        }

        if(variables.apply(c.v()).size() == 1) {
            if(vals(c.u()).contains((Integer) vals(c.v()).values.head())) {
                variables = variables.updated(c.u(), vals(c.u()).remove(vals(c.v())));
                for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(c.u())))
                    if(neighbour != c.v())
                        toProcess.add(new Pair<>(neighbour, c.u()));
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
            for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(c.v())))
                if(neighbour != c.u())
                    toProcess.add(new Pair<>(neighbour, c.v()));
            for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(c.u())))
                if(neighbour != c.v())
                    toProcess.add(new Pair<>(neighbour, c.u()));

            if(newDomain.values.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Revise all extension constraints involving the two variables. It is asymmetrical and
     * only the domain of the first variable will be reduced.
     *
     * If the domain of var changed, the edges to reconsider are added to the queue.
     *
     * @param var Variable whose domain is to check for reduction.
     * @param other Other variable for which values of var need to be checked.
     * @return True if the domain is non-empty after the propagation.
     */
    protected boolean reviseExts(VarRef var, VarRef other) {
        assert variables.contains(var);
        assert variables.contains(other);
        boolean updated = false;
        for(ExtensionConstraint ext : exts) {
            if(ext.isAbout(var) && ext.isAbout(other)) {
                Map<VarRef,Map<Integer, List<Integer>>> constraintsPerVar = ext.processed().get(var);
                LinkedList<Integer> toRemove = new LinkedList<>();
                Map<Integer, List<Integer>> constraints = constraintsPerVar.get(other);
                for(Object vObj : vals(var).values()) {
                    Integer v = (Integer) vObj;
                    if(!constraints.containsKey(v) || !vals(other).containsAtLeastOne(constraints.get(v)))
                        toRemove.add(v);
                }
                while(!toRemove.isEmpty()) {
                    updated = true;
                    variables = variables.updated(var, vals(var).remove(toRemove.remove()));
                }
            }
        }

        if(updated) {
            for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(var)))
                if(neighbour != other)
                    toProcess.add(new Pair<>(neighbour, var));
        }
        return !vals(var).isEmpty();
    }

    @Override
    public boolean restrictDomain(VarRef var, Collection<String> toValues) {
        assert variables.contains(var);
        List<Integer> ids = new LinkedList<>();
        for(String value : toValues) {
            ids.add(valuesIds.get(value));
        }
        if(vals(var).equals(new ValuesHolder(ids)))
            return isConsistent();

        variables = variables.updated(var, variables.apply(var).intersect(new ValuesHolder(ids)));

        for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(var)))
            toProcess.add(new Pair<>(neighbour, var));

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
        toProcess.add(new Pair<>(a, b));
        isConsistent();
    }

    @Override
    public void AddSeparationConstraint(VarRef a, VarRef b) {
        LabeledEdge<VarRef, ConstraintType> constraint = new LabeledEdge<VarRef, ConstraintType>(a, b, ConstraintType.DIFFERENCE);
        constraints.addEdge(a, b, ConstraintType.DIFFERENCE);
        toProcess.add(new Pair<>(a, b));
        isConsistent();
    }

    public void addExtensionConstraint(List<VarRef> vars, List<List<String>> values) {
        LinkedList<LinkedList<Integer>> intValues = new LinkedList<>();
        for(List<String> valSeq : values) {
            LinkedList<Integer> intValSeq = new LinkedList<>();
            for(String v : valSeq) {
                intValSeq.add(valuesIds.get(v));
            }
            assert intValSeq.size() == vars.size();
            intValues.add(intValSeq);
        }
        exts.add(new ExtensionConstraint(vars, intValues));
        for(int i=0 ; i<vars.size() ; i++) {
            for(int j=i+1 ; j<vars.size() ; j++) {
                constraints.addEdge(vars.get(i), vars.get(j), ConstraintType.EXTENSION);
            }
        }
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
            else if(c.l() == ConstraintType.EXTENSION)
                builder.append(" EXTENSION ");
            builder.append(c.v()+":");
            builder.append(domainOf(c.v()));
            builder.append("\n");
        }
        builder.append("\n----------------\n");
        return builder.toString();
    }

}
