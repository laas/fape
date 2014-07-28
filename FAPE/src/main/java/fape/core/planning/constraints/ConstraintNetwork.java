package fape.core.planning.constraints;

import fape.exceptions.FAPEException;
import fape.util.Reporter;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.VarRef;

import java.util.List;

public class ConstraintNetwork extends ConservativeConstraintNetwork<VarRef> implements Reporter {

    public ConstraintNetwork() {
        super();
    }

    public ConstraintNetwork(ConstraintNetwork toCopy) {
        super(toCopy);
    }

    public ConstraintNetwork DeepCopy() {
        return new ConstraintNetwork(this);
    }

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
}
