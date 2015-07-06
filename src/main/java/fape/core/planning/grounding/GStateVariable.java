package fape.core.planning.grounding;

import planstack.anml.model.Function;
import planstack.anml.model.concrete.InstanceRef;

import java.util.Arrays;

public class GStateVariable {

    final public Function f;
    final public InstanceRef[] params;

    int hashVal;

    public GStateVariable(Function f, InstanceRef[] params) {
        this.f = f;
        this.params = params.clone();

        int i = 0;
        hashVal = 0;
        hashVal += f.hashCode() * 42*i++;
        for(InstanceRef param : this.params) {
            hashVal += param.hashCode() * 42*i++;
        }
    }


    @Override
    public boolean equals(Object o) {
        if(!(o instanceof GStateVariable))
            return false;
        GStateVariable oSV = (GStateVariable) o;

        if(!f.equals(oSV.f))
            return false;

        for(int i=0 ; i<params.length ; i++) {
            if(!params[i].equals(oSV.params[i]))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return f.name() + Arrays.toString(params);
    }

    @Override
    public int hashCode() {
        return hashVal;
    }
}
