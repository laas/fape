package fape.core.planning.grounding;

import fape.core.inference.Term;
import fape.core.planning.planninggraph.PGNode;
import planstack.anml.model.Function;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.parser.Instance;

import java.util.Arrays;

public class Fluent implements PGNode, Term {
    final public GStateVariable sv;
    final public InstanceRef value;
    final public boolean partOfTransition;

    public Fluent(GStateVariable sv, InstanceRef value, boolean partOfTransition) {
        this.sv = sv;
        this.value = value;
        this.partOfTransition = partOfTransition;
    }

    public Fluent(Function f, VarRef[] params, VarRef value, boolean partOfTransition) {
        this.partOfTransition = partOfTransition;
        InstanceRef[] castParams = new InstanceRef[params.length];
        for(int i=0 ; i< params.length ; i++)
            castParams[i] = (InstanceRef) params[i];
        this.sv = new GStateVariable(f, castParams);
        this.value = (InstanceRef) value;
    }

    @Override
    public String toString() {
        return sv +"=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Fluent))
            return false;
        Fluent of = (Fluent) o;
//        if(partOfTransition != of.partOfTransition)
//            return false;

        return sv.equals(of.sv) && value.equals(of.value);

    }

    @Override
    public int hashCode() {
        return sv.hashCode() * 42* value.hashCode();
    }

}
