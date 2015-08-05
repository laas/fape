package planstack.constraints.bindings;

import planstack.graph.core.LabeledEdge;
import planstack.graph.core.impl.MultiLabeledUndirectedAdjacencyList;
import scala.collection.JavaConversions;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

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
public interface BindingCN<VarRef> {

    enum ConstraintType { EQUALITY, DIFFERENCE, NO_CONSTRAINT }
    /**
     * Defines a listener that will be updated every time a variable is binded.
     * If a variable was already binded before that, no notification will be issued.
     * @param listener Object to be notified.
     */
    public void setListener(IntBindingListener<VarRef> listener);



    public void addPossibleValue(String val);

    public void addPossibleValue(int val);

    /**
     * Checks the consistency of the binding constraints network.
     * If needed, a propagation is performed.
     * @return True if it is consistent (no variables with empty domain).
     */
    public boolean isConsistent();

    /** Domain representation of a set of integer values */
    public ValuesHolder intValuesAsDomain(Collection<Integer> intDomain);

    /** Domain representation of a set of string values */
    public ValuesHolder stringValuesAsDomain(Collection<String> stringDomain);

    /**
     *  Restricts the domain of variable to the given one.
     *  The resulting domain will be the intersection of the existing and this one.
     */
    public boolean restrictDomain(VarRef v, ValuesHolder domain);

    public void restrictDomain(VarRef var, Collection<String> toValues);

    public void restrictIntDomain(VarRef var, Collection<Integer> toValues);

    /** Only keep values in var's domain that lower or equal to max. var must be an integer variable. */
    public void keepValuesBelowOrEqualTo(VarRef var, int max);

    /** Only keep values in var's domain that greater or equal to min. var must be an integer variable. */
    public void keepValuesAboveOrEqualTo(VarRef var, int min);


    public void AddVariable(VarRef var, Collection<String> domain, String type);

    /**
     * Creates a new integer variable. Its domain will contain all integer values that where declared up to now.
     * @param var Variable to record.
     */
    public void AddIntVariable(VarRef var);

    public void AddIntVariable(VarRef var, Collection<Integer> domain);

    public void AddUnificationConstraint(VarRef a, VarRef b);

    public void AddSeparationConstraint(VarRef a, VarRef b);

    public boolean isRecorded(VarRef v);

    /** Returns true if the variable has already been declared. */
    public boolean contains(VarRef v);
    /** Human readable representation of a domain. */
    public String domainAsString(VarRef v);

    /** number of values in the domain of this variable */
    public Integer domainSize(VarRef var);
    /**
     * Returns the domain of a variable (except for integer variables).
     *
     * Complexity is O(|dom(var)|) since a translation of every value of the domain is necessary
     */
    public List<String> domainOf(VarRef var);

    /**
     * Returns the domain of an integer variable.
     *
     * Complexity is O(|dom(var)|) since a translation of every value of the domain is necessary
     */
    public List<Integer> domainOfIntVar(VarRef var);

    public ValuesHolder rawDomain(VarRef var);

    /**
     * Returns the type of this variable.
     */
    public String typeOf(VarRef v);

    /**
     * Returns true if the domain of v is of type Integer
     */
    public boolean isIntegerVar(VarRef v);
    /**
     * Makes an independent copy of this binding constraint network.
     */
    public BindingCN<VarRef> DeepCopy();
    public boolean unifiable(VarRef a, VarRef b);

    /** Returns true if (i) there is no previous separation constraint between the two variables
     * (ii) adding such a constraint would not result in a trivially inconsistent network.
     */
    public boolean separable(VarRef a, VarRef b);

    public boolean unified(VarRef a, VarRef b);

    public boolean separated(VarRef a, VarRef b);

    /**
     * @return All non-integer variables whose domain is not a singleton.
     */
    public List<VarRef> getUnboundVariables();

    /**
     * Adds a new tuple of value to an extension constraint.
     * If no constraint with this ID exists, it is created.
     * @param setID ID of the extension constraint.
     * @param values A tuple of value to be added.
     */
    public void addValuesToValuesSet(String setID, List<String> values);

    /**
     * Adds a new tuple of value to an extension constraint.
     * If no constraint with this ID exists, it is created.
     * @param setID ID of the extension constraint.
     * @param values A tuple of value to be added.
     */
    public void addValuesToValuesSet(String setID, List<String> values, int lastVal);

    /**
     * Forces the given tuple of variable to respect an extension constraint
     * @param variables a tuple of variable.
     * @param setID ID of the extension cosntraint to respect.
     */
    public void addValuesSetConstraint(List<VarRef> variables, String setID);
    public String Report();

    public void assertGroundAndConsistent();

    public Integer intValueOfRawID(Integer valueID);
}
