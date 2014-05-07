package fape.core.planning.constraints;

import fape.exceptions.FAPEException;
import fape.util.Reporter;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.VarRef;

import java.util.Collection;
import java.util.List;

public abstract class ConstraintNetwork implements Reporter {

        /**
     * @return True if the network is consistent (no vars with empty domain),
     *         False otherwise
     */
    public abstract boolean isConsistent();

    /**
     * Restricts the domain of a variable to the given values.
     * The resulting domain of the variable will be set to the intersection of its current one
     * and of the given values.
     * @param var Variable whose domain is to be restricted.
     * @param values allowed values for this variables.
     * @return
     */
    public abstract boolean restrictDomain(VarRef var, Collection<String> values);

    /**
     * Records that a value can appear in a domain.
     * Some underlying implementations don't use this method be it should be invoked
     * for every value appearing in a domain.
     * @param val Value to be recorded.
     */
    public void addPossibleValue(String val) {}


    /**
     * Records a new variable in the CSP
     * @param var Reference of the variable
     * @param domain All elements in the domain of the variable
     */
    public abstract void AddVariable(VarRef var, Collection<String> domain, String type);

    /**
     * Adds a constraints stating that are a and b are identical.
     * @param a
     * @param b
     */
    public abstract void AddUnificationConstraint(VarRef a, VarRef b);


    /**
     * Adds unification constraints for all variables
     * Passing the lists <code>[a1, a2], [b1, b2]</code> as arguments will result in the constraints
     * a1 == b1 and a2 == b2
     * @param as List of variables
     * @param bs List of variables
     */
    public void AddUnificationConstraints(List<VarRef> as, List<VarRef> bs) {
        assert as.size() == bs.size();
        for(int i=0 ; i < as.size() ; i++) {
            AddUnificationConstraint(as.get(i), bs.get(i));
        }
    }

    /**
     * Unifies, two-by-two, the arguments of the two state variables.
     * Note that the two state variables must be on the same function.
     */
    public void AddUnificationConstraint(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        if(!a.func().equals(b.func()))
            throw new FAPEException("Error: adding unification constraint between two different predicates: "+ a +"  --  "+ b);
        AddUnificationConstraints(a.jArgs(), b.jArgs());
    }

    /**
     * @param v Variable to look up
     * @return True if variable v is declared in the CSP.
     */
    public abstract boolean contains(VarRef v);

    /**
     * Get the domain of a variable.
     * Depending on the underlying implementation, this method might require a translation
     * from the internal CSP representation. Hence it might not be fast and its use should be avoided in
     * critical areas.
     * @param v Variable whose domain is to be retrieved.
     * @return The domain of the variable.
     */
    public abstract Collection<String> domainOf(VarRef v);

    /**
     * Returns the type of the given variable
     * @param v Variable to look up
     * @return Type of the variable
     */
    public abstract String typeOf(VarRef v);

    /**
     * Makes a copy of the CSP.
     * @return A new COnstraintNetwork object with the same content.
     */
    public abstract ConstraintNetwork DeepCopy();

    /**
     * Checks if the addition of an equality constraint between two variables is possible.
     * This is done by checking if the intersection of their domains is non-empty.
     * @return True if the two variable are unifiable.
     */
    public abstract boolean unifiable(VarRef a, VarRef b);

}
