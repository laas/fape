package fape.core.planning.planninggraph;

import planstack.anml.model.Function;

import java.util.LinkedList;
import java.util.List;

public class Fluent {
    final Function f;
    final List<String> params;
    final String value;

    int hashVal;


    public Fluent(Function f, List<String> params, String value) {
        this.f = f;
        this.params = new LinkedList<String>(params);
        this.value = value;

        int i = 0;
        hashVal = 0;
        hashVal += f.hashCode() * 42*i++;
        hashVal += value.hashCode() * 42*i++;
        for(String param : params) {
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
        if(!f.equals(of.f))
            return false;

        for(int i=0 ; i<params.size() ; i++) {
            if(!params.get(i).equals(of.params.get(i)))
                return false;
        }

        return value.equals(of.value);
    }

    @Override
    public int hashCode() {
        return hashVal;
    }

}
