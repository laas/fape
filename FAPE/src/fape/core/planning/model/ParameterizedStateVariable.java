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

    public ParameterizedStateVariable(String predicate, VariableRef var, String type) {
        this.predicateName = predicate;
        this.variable = var;
        this.type = type;
    }

    @Override
    public String toString() {
        return variable.toString() + "." + predicateName;
    }
}
