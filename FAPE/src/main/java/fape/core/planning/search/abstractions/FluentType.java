package fape.core.planning.search.abstractions;


import fape.core.planning.model.ParameterizedStateVariable;
import fape.core.planning.model.VariableRef;

/**
 * Encodes the type of "fluent".
 *  - name of the predicate (part of the state variable)
 *  - type of the argument of the predicate/state variable
 *  - type of the value
 */
class FluentType {
    public final String predicateName;
    public final String argType;
    public final String valueType;

    public FluentType(ParameterizedStateVariable sv, VariableRef value) {
        this.predicateName = sv.predicateName;
        this.argType = sv.variable.type;
        this.valueType = value.type;
    }

    public FluentType(String predicate, String argType, String valueType) {
        this.predicateName = predicate;
        this.argType = argType;
        this.valueType = valueType;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof FluentType)) {
            return false;
        } else {
            FluentType ft = (FluentType) o;
            return predicateName.equals(ft.predicateName) &&
                    argType.equals(ft.argType) &&
                    valueType.equals(ft.valueType);
        }
    }

    @Override
    public int hashCode() {
        return predicateName.hashCode() * 42 * 42 +
                argType.hashCode() * 42 + valueType.hashCode();
    }

    @Override
    public String toString() {
        return predicateName+"("+argType+"):"+valueType;
    }
}