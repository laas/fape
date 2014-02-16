package fape.core.planning.model;

import fape.core.execution.model.Reference;
import fape.exceptions.FAPEException;


/**
 * Contains a reference to an object variable.
 * This is an immutable class, hence values of the object variable
 * are to be stored elsewhere (typically in the constraint manager.
 *
 * The null variable is defined to be anything and is represented by a variable whose name is "null".
 *
 * TODO: handle tye of the variable
 */
public class VariableRef {

    public final String var;

    public VariableRef(String var) {
        this.var = var;
    }

    public VariableRef(Reference ref) {
        if(ref.refs.size() != 1)
            throw new FAPEException("Cannot create variable from reference: " + ref);
        this.var = ref.GetConstantReference();
    }

    public Reference GetReference() {
        return new Reference(var);
    }

    public boolean isNull() {
        return this.var.equals("null");
    }

    @Override
    public String toString() {
        return var;
    }

    @Override
    public int hashCode() {
        return var.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof  String) {
            return o.equals(var);
        } else if(o instanceof VariableRef) {
            return ((VariableRef) o).var.equals(var);
        } else {
            return false;
        }
    }
}
