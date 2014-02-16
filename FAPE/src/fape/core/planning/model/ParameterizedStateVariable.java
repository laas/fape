package fape.core.planning.model;

import fape.core.execution.model.Reference;
import fape.exceptions.FAPEException;


/**
 * A parameterized state variable is of the form predicate(x) where x is an object variable.
 *
 * It maps to the anml notation x.predicate
 */
public class ParameterizedStateVariable {

    public final String predicateName;
    public final VariableRef variable;
    public final String type;

    public ParameterizedStateVariable(Reference ref, String type) {
        variable = new VariableRef(ref.GetConstantReference());
        if(ref.refs.size() == 1) {
            predicateName = "";
        } else if(ref.refs.size() == 2) {
            predicateName = ref.refs.get(1);
        } else {
            throw new FAPEException("Reference is too long to build a stateVariable: " + ref);
        }
        this.type = type;
    }

    @Override
    public String toString() {
        return variable.toString() + "." + predicateName;
    }
}
