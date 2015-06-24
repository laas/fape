package fape.core.planning.grounding;

import fape.core.inference.Term;
import fape.core.planning.planninggraph.PGNode;
import planstack.anml.model.Function;
import planstack.anml.model.concrete.VarRef;

public class Fluent implements PGNode, Term {
    final public Function f;
    final public VarRef[] params;
    final public VarRef value;
    final public boolean partOfTransition;

    int hashVal;


    public Fluent(Function f, VarRef[] params, VarRef value, boolean partOfTransition) {
        this.partOfTransition = partOfTransition;
        this.f = f;
        this.params = params;
        this.value = value;

        int i = 0;
        hashVal = 0;
        hashVal += f.hashCode() * 42*i++;
        hashVal += value.hashCode() * 42*i++;
        for(VarRef param : params) {
            hashVal += param.hashCode() * 42*i++;
        }
    }

    @Override
    public String toString() {
        return f.name() + params.toString() +"=" + value;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Fluent))
            return false;
        Fluent of = (Fluent) o;
//        if(partOfTransition != of.partOfTransition)
//            return false;

        if(!f.equals(of.f))
            return false;

        for(int i=0 ; i<params.length ; i++) {
            if(!params[i].equals(of.params[i]))
                return false;
        }

        return value.equals(of.value);
    }

    @Override
    public int hashCode() {
        return hashVal;
    }

}
