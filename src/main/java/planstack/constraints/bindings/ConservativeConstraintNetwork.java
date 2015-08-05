package planstack.constraints.bindings;

import planstack.graph.core.LabeledEdge;
import planstack.graph.core.impl.MultiLabeledUndirectedAdjacencyList;
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
public class ConservativeConstraintNetwork<VarRef> implements BindingCN<VarRef> {

    enum ConstraintType { EQUALITY, DIFFERENCE }

    /** A listener that will be notified every time a variable is binded.
     * No notification will be issued for variables that were binded before
     * this variable is set to a non-null value.
     */
    public IntBindingListener<VarRef> listener = null;

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
    public final ArrayList<String> values;

    /**
     * Maps every value to its ID.
     * Shared between all instances.
     */
    final HashMap<String, Integer> valuesIds;

    public final ArrayList<Integer> intValues;

    /**
     * Contains a domain that is shared between all instances of ConservativeConstraintNetworks.
     * This domain is the default one for all new int variables.
     */
    private final ValuesHolder[] defaultIntDomain;

    final HashMap<Integer, Integer> intValuesIds;

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
    public final HashMap<String, ExtensionConstraint> exts;
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

    /**
     * This boolean is used to make sure we don't add possible values to an extension constraints
     * after a propagation has occured.
     */
    public boolean extChecked;

    public ConservativeConstraintNetwork() {
        toProcess = new LinkedList<>();
        variables = new scala.collection.immutable.HashMap<>();
        types = new HashMap<>();
        values = new ArrayList<>();
        valuesIds = new HashMap<>();
        intValues = new ArrayList<>();
        intValuesIds = new HashMap<>();
        constraints = new MultiLabeledUndirectedAdjacencyList<>();
        consistent = true;
        exts = new HashMap<String, ExtensionConstraint>();
        mappings = new HashMap<>();
        extChecked = false;
        defaultIntDomain = new ValuesHolder[1];
        defaultIntDomain[0] = new ValuesHolder(new LinkedList<Integer>());
        assert defaultIntDomain[0].size() == intValues.size();
    }

    public ConservativeConstraintNetwork(ConservativeConstraintNetwork<VarRef> base) {
        variables = base.variables;
        types = base.types; // todo, need to copy?
        values = base.values;
        valuesIds = base.valuesIds;
        intValues = base.intValues;
        intValuesIds = base.intValuesIds;
        constraints = base.constraints.cc();
        toProcess = new LinkedList<VarRef>(base.toProcess);
        consistent = base.consistent;
        mappings = new HashMap<List<VarRef>, String>(base.mappings);
        exts = base.exts;
        extChecked = base.extChecked;
        defaultIntDomain = base.defaultIntDomain;
        assert defaultIntDomain[0].size() == intValues.size();
    }

    /**
     * Defines a listener that will be updated every time a variable is binded.
     * If a variable was already binded before that, no notification will be issued.
     * @param listener Object to be notified.
     */
    public void setListener(IntBindingListener<VarRef> listener) {
        this.listener = listener;
    }

    /** Invoked whenever the domain of a variable is modified (including on variable creation)
     *
     * The variable is added in the agenda for propagation.
     * If the domain is empty, the CSP is set to inconsistent.
     * If the domain is a singleton (variable is binded), the listener is notified of that binding.
     * @param v Variable whose domain changed.
     */
    private void domainModified(VarRef v) {
        if(!toProcess.contains(v))
            toProcess.add(v);

        if(domainSize(v) == 0) {
            consistent = false;
        }

        if(listener != null && domainSize(v) == 1 && isIntegerVar(v)) {
            listener.onBinded(v, domainOfIntVar(v).get(0));
        }
    }

    protected void checkValuesSetConstraints(VarRef v) {
        if(domainSize(v) != 1)
            return;

        List<List<VarRef>> constraintsToCheck = new LinkedList<>();
        for(List<VarRef> constraint : mappings.keySet()) {
            if(constraint.contains(v))
                constraintsToCheck.add(constraint);
        }

        for(List<VarRef> cons : constraintsToCheck) {
            Set<Integer>[] domains = new Set[cons.size()];
            for(int j=0 ; j<cons.size() ; j++) {
                domains[j] = variables.apply(cons.get(j)).values();
            }
            // get restricted domains for all vars in the constraint
            Set<Integer>[] restrictedDomains = exts.get(mappings.get(cons)).restrictedDomains(domains);

            for(int i=0 ; i<cons.size() ; i++) {
                assert cons.size() == restrictedDomains.length;
                VarRef focusVar = cons.get(i);

                ValuesHolder old = variables.apply(focusVar);
                variables = variables.updated(focusVar, variables.apply(focusVar).intersect(new ValuesHolder(restrictedDomains[i])));
                if (old.size() > variables.apply(focusVar).size())
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

    public void addPossibleValue(int val) {
        if(!intValuesIds.containsKey(val)) {
            int id = intValues.size();
            intValues.add(id, val);
            intValuesIds.put(val, id);
            defaultIntDomain[0] = defaultIntDomain[0].add((Integer) id);
            assert defaultIntDomain[0].size() == intValues.size();
        }
    }

    /**
     * Checks the consistency of the binding constraints network.
     * If needed, a propagation is performed.
     * @return True if it is consistent (no variables with empty domain).
     */
    public boolean isConsistent() {
        if(!consistent) {
            return false;
        }

        while(!toProcess.isEmpty()) {
            VarRef v = toProcess.remove();
            consistent &= revise(v);
            checkValuesSetConstraints(v);
            extChecked = true; // note that a propagation occurred
        }
        return consistent;
    }

    private ValuesHolder vals(VarRef var) {
        return variables.apply(var);
    }

    /**
     * Revises all equality and difference constraints in which a variable appears
     * @param v Focus variable.
     * @return If no inconsistency was detected during propagation.
     */
    protected boolean revise(VarRef v) {
        boolean res = true;
        for(VarRef neighbour : JavaConversions.asJavaCollection(constraints.neighbours(v)))
            res &= revise(v, neighbour);

        return res;
    }

    /**
     * Revises all equality and difference constraints in which both variables appear.
     *
     * @return True if no inconsistency was detected (ie both v1 and v2 have non empty domains)
     */
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

    /** Domain representation of a set of integer values */
    public ValuesHolder intValuesAsDomain(Collection<Integer> intDomain) {
        List<Integer> ids = new LinkedList<>();
        for(Integer value : intDomain) {
            ids.add(intValuesIds.get(value));
        }
        return new ValuesHolder(ids);
    }

    /** Domain representation of a set of string values */
    public ValuesHolder stringValuesAsDomain(Collection<String> stringDomain) {
        List<Integer> ids = new LinkedList<>();
        for(String value : stringDomain) {
            ids.add(valuesIds.get(value));
        }
        return new ValuesHolder(ids);
    }

    /**
     *  Restricts the domain of variable to the given one.
     *  The resulting domain will be the intersection of the existing and this one.
     */
    public boolean restrictDomain(VarRef v, ValuesHolder domain) {
        ValuesHolder old = variables.apply(v);
        ValuesHolder newDom = old.intersect(domain);
        variables = variables.updated(v, newDom);

        if(newDom.isEmpty()) {
            consistent = false;
            return true;
        } else if(old.size() != newDom.size()) {
            domainModified(v);
            return true;
        } else {
            return false;
        }
    }

    protected boolean reviseDifference(LabeledEdge<VarRef, ConstraintType> c) {
        assert c.l() == ConstraintType.DIFFERENCE;

        if(variables.apply(c.u()).size() == 1) {
            if(vals(c.v()).contains((Integer) vals(c.u()).head())) {
                boolean changed = restrictDomain(c.v(), vals(c.v()).remove(vals(c.u())));
                assert changed;
            }
        }

        if(variables.apply(c.v()).size() == 1) {
            if(vals(c.u()).contains((Integer) vals(c.v()).head())) {
                boolean changed = restrictDomain(c.u(), vals(c.u()).remove(vals(c.v())));
                assert changed;
            }
        }

        return !vals(c.u()).isEmpty() && !vals(c.v()).isEmpty();
    }

    protected boolean reviseEquality(LabeledEdge<VarRef, ConstraintType> c) {
        assert c.l() == ConstraintType.EQUALITY;

        if(!variables.apply(c.u()).equals(variables.apply(c.v()))) {
            restrictDomain(c.u(), variables.apply(c.v()));
            restrictDomain(c.v(), variables.apply(c.u()));
        }
        return consistent;
    }

    public void restrictDomain(VarRef var, Collection<String> toValues) {
        assert variables.contains(var);
        assert !isIntegerVar(var);
        restrictDomain(var, stringValuesAsDomain(toValues));
    }

    public void restrictIntDomain(VarRef var, Collection<Integer> toValues) {
        assert variables.contains(var);
        assert isIntegerVar(var);
        restrictDomain(var, intValuesAsDomain(toValues));
    }

    @Override
    public void keepValuesBelowOrEqualTo(VarRef var, int max) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void keepValuesAboveOrEqualTo(VarRef var, int min) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void AddVariable(VarRef var, Collection<String> domain, String type) {
        assert !type.equals("integer") && !type.equals("int");
        assert !variables.contains(var);
        List<Integer> valueIds = new LinkedList<>();
        for(String val : domain) {
            assert valuesIds.containsKey(val) : "Error: "+val+" is not known to the constraint network. "+
                    "Make sure no variable is defined before all of the instances are.\n"+
                    "For instance \"instance A a1, a2, a3;\" and constant A v;\" should be in the same file.";
            valueIds.add(valuesIds.get(val));
        }
        variables = variables.updated(var, new ValuesHolder(valueIds));
        types.put(var, type);
        domainModified(var);
        constraints.addVertex(var);
    }

    /**
     * Creates a new integer variable. Its domain will all integer values that where delared up to now.
     * @param var Variable to record.
     */
    public void AddIntVariable(VarRef var) {
        addIntVariable(var, defaultIntDomain[0]);
        assert defaultIntDomain[0].size() == intValues.size();
    }

    private void addIntVariable(VarRef var, ValuesHolder domain) {
        assert !variables.contains(var);
        variables = variables.updated(var, domain);
        types.put(var, "integer");
        domainModified(var);
        constraints.addVertex(var);
    }

    public void AddIntVariable(VarRef var, Collection<Integer> domain) {
        List<Integer> valueIds = new LinkedList<>();
        for(Integer val : domain) {
            addPossibleValue(val);
            valueIds.add(intValuesIds.get(val));
        }
        addIntVariable(var, new ValuesHolder(valueIds));
    }

    public void AddUnificationConstraint(VarRef a, VarRef b) {
        LabeledEdge<VarRef, ConstraintType> constraint = new LabeledEdge<VarRef, ConstraintType>(a, b, ConstraintType.EQUALITY);
        constraints.addEdge(a, b, ConstraintType.EQUALITY);
        toProcess.add(a);
        toProcess.add(b);
    }

    public void AddSeparationConstraint(VarRef a, VarRef b) {
        LabeledEdge<VarRef, ConstraintType> constraint = new LabeledEdge<VarRef, ConstraintType>(a, b, ConstraintType.DIFFERENCE);
        constraints.addEdge(a, b, ConstraintType.DIFFERENCE);
        toProcess.add(a);
        toProcess.add(b);
    }

    @Override
    public boolean isRecorded(VarRef v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Returns true if the variable has already been declared. */
    public boolean contains(VarRef v) {
        return variables.contains(v);
    }

    /** Human readable representation of a domain. */
    public String domainAsString(VarRef v) {
        if(isIntegerVar(v)) {
            return domainOfIntVar(v).toString();
        } else {
            return domainOf(v).toString();
        }
    }

    /** number of values in the domain of this variable */
    public Integer domainSize(VarRef var) {
        return variables.apply(var).size();
    }

    /**
     * Returns the domain of a variable (except for integer variables).
     *
     * Complexity is O(|dom(var)|) since a translation of every value of the domain is necessary
     */
    public List<String> domainOf(VarRef var) {
        assert !isIntegerVar(var);
        List<String> domain = new LinkedList<>();
        for(Integer id : variables.apply(var).values()) {
            domain.add(values.get(id));
        }
        return domain;
    }

    /**
     * Returns the domain of an integer variable.
     *
     * Complexity is O(|dom(var)|) since a translation of every value of the domain is necessary
     */
    public List<Integer> domainOfIntVar(VarRef var) {
        assert isIntegerVar(var);
        List<Integer> domain = new LinkedList<>();
        for(Integer id : variables.apply(var).values()) {
            domain.add(intValues.get(id));
        }
        return domain;
    }

    public ValuesHolder rawDomain(VarRef var) {
        return variables.apply(var);
    }

    /**
     * Returns the type of this variable.
     */
    public String typeOf(VarRef v) {
        assert types.containsKey(v);
        return types.get(v);
    }

    /**
     * Returns true if the domain of v is of type Integer
     */
    public boolean isIntegerVar(VarRef v) {
        assert !typeOf(v).equals("int");
        return typeOf(v).equals("integer");
    }

    /**
     * Makes an independent copy of this binding constraint network.
     */
    public ConservativeConstraintNetwork<VarRef> DeepCopy() {
        return new ConservativeConstraintNetwork<VarRef>(this);
    }

    public boolean unifiable(VarRef a, VarRef b) {
        return !separated(a,b) && variables.apply(a).intersect(variables.apply(b)).size() != 0;
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
        if(a == b)
            return true;
        if(domainSize(a) == 1 && domainSize(b) == 1 && domainOf(a).get(0).equals(domainOf(b).get(0)))
            return true;

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

    /**
     * @return All non-integer variables whose domain is not a singleton.
     */
    public List<VarRef> getUnboundVariables() {
        List<VarRef> unbound = new LinkedList<>();
        for(VarRef var : JavaConversions.asJavaCollection(variables.keys().toList())) {
            if(domainSize(var) > 1 && !isIntegerVar(var))
                unbound.add(var);
        }
        return unbound;
    }

    /**
     * Adds a new tuple of value to an extension constraint.
     * If no constraint with this ID exists, it is created.
     * @param setID ID of the extension constraint.
     * @param values A tuple of value to be added.
     */
    public void addValuesToValuesSet(String setID, List<String> values) {
        List<Integer> vals = new LinkedList<>();
        for(String v : values) {
            assert valuesIds.containsKey(v);
            vals.add(valuesIds.get(v));
        }
        if(!exts.containsKey(setID)) {
            exts.put(setID, new ExtensionConstraint(false));
        }

        exts.get(setID).addValues(vals);
        assert !extChecked : "Error: adding values to constraints in extension while propagation already occurred.";
    }

    /**
     * Adds a new tuple of value to an extension constraint.
     * If no constraint with this ID exists, it is created.
     * @param setID ID of the extension constraint.
     * @param values A tuple of value to be added.
     */
    public void addValuesToValuesSet(String setID, List<String> values, int lastVal) {
        List<Integer> vals = new LinkedList<>();
        for(String v : values) {
            assert valuesIds.containsKey(v);
            vals.add(valuesIds.get(v));
        }
        assert intValuesIds.get(lastVal) != null : "Integer value "+lastVal+" was not recorded";
        vals.add(intValuesIds.get(lastVal));
        if(!exts.containsKey(setID)) {
            exts.put(setID, new ExtensionConstraint(true));
        }

        exts.get(setID).addValues(vals);
        assert !extChecked : "Error: adding bindings to constraints in extension while propagation already occurred.";
    }

    /**
     * Forces the given tuple of variable to respect an extension constraint
     * @param variables a tuple of variable.
     * @param setID ID of the extension cosntraint to respect.
     */
    public void addValuesSetConstraint(List<VarRef> variables, String setID) {
        extChecked = true;
        if(!exts.containsKey(setID)) {
            exts.put(setID, new ExtensionConstraint(isIntegerVar(variables.get(variables.size()-1))));
        }

        assert exts.get(setID).isEmpty() || exts.get(setID).numVars() == variables.size();
        assert exts.get(setID).isLastVarInteger == isIntegerVar(variables.get(variables.size()-1));
        mappings.put(new LinkedList<VarRef>(variables), setID);
        LinkedList<HashSet<Integer>> domains = new LinkedList<>();
        for(int i=0 ; i< variables.size() ; i++) {
            domains.add(new HashSet<Integer>());
        }
        for(int[] binding : exts.get(setID).bindings) {
            if(binding != null) {
                for (int i = 0; i < binding.length; i++) {
                    domains.get(i).add(binding[i]);
                }
            }
        }
        for(int i=0 ; i<variables.size() ; i++) {
            VarRef var = variables.get(i);
            HashSet<Integer> domain = domains.get(i);
            this.restrictDomain(var, new ValuesHolder(domain));
        }
        for(VarRef v : variables) {
            domainModified(v);
        }
    }

    public String Report() {
        StringBuilder builder = new StringBuilder();
        for(VarRef var : JavaConversions.asJavaCollection(variables.keys())) {
            builder.append(var);
            builder.append("  ");
            builder.append(domainAsString(var));
            builder.append("\n");
        }
        builder.append("\n");
        for(LabeledEdge<VarRef,ConstraintType> c : constraints.jEdges()) {
            builder.append(c.u()+":");
            builder.append(domainAsString(c.u()));
            if(c.l() == ConstraintType.DIFFERENCE)
                builder.append(" != ");
            else if(c.l() == ConstraintType.EQUALITY)
                builder.append(" == ");
            builder.append(c.v()+":");
            builder.append(domainAsString(c.v()));
            builder.append("\n");
        }
        builder.append("\n----------------\n");
        return builder.toString();
    }

    public void assertGroundAndConsistent() {
        for(ValuesHolder vals : JavaConversions.asJavaCollection(variables.values()))
            assert vals.size() == 1;

        for(List<VarRef> extConst : mappings.keySet()) {
            List<Integer> vals = new LinkedList<>();
            for(VarRef v : extConst) vals.add(variables.apply(v).values().iterator().next());

            boolean isValidTuple = false;
            for(int[] valsTuple : exts.get(mappings.get(extConst)).bindings) {
                if(valsTuple != null) {
                    for (int i = 0; i < valsTuple.length; i++) {
                        if (!vals.get(i).equals(valsTuple[i]))
                            break;
                        if (i == valsTuple.length - 1)
                            isValidTuple = true;
                    }
                    if (isValidTuple)
                        break;
                }
            }
            if(!isValidTuple)
                assert isValidTuple;
        }

        for(LabeledEdge<VarRef, ConstraintType> e : constraints.jEdges()) {
            int v1 = variables.apply(e.u()).values().iterator().next();
            int v2 = variables.apply(e.v()).values().iterator().next();

            if(e.l() == ConstraintType.EQUALITY)
                assert v1 == v2;
            else if(e.l() == ConstraintType.DIFFERENCE)
                assert v1 != v2;
        }
    }

    @Override
    public Integer intValueOfRawID(Integer valueID) {
        return intValues.get(valueID);
    }

}
