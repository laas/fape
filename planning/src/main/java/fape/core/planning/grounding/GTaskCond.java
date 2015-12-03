package fape.core.planning.grounding;

import planstack.anml.model.concrete.InstanceRef;

import java.util.Collection;

public class GTaskCond {
    public final String name;
    public final InstanceRef[] args;

    public GTaskCond(String name, Collection<InstanceRef> args) {
        this.name = name;
        this.args = new InstanceRef[args.size()];
        int i=0;
        for(InstanceRef arg : args) {
            this.args[i] = arg;
            i++;
        }
    }

    @Override
    public String toString() {
        String ret = name+"(";
        for(InstanceRef arg : args)
            ret+= arg+",";
        return ret+")";
    }

    @Override
    public int hashCode() {
        int ret = name.hashCode();
        for(int i=0 ; i<args.length ; i++) {
            ret += 42^i * args[i].hashCode();
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof GTaskCond) {
            GTaskCond gtc = (GTaskCond) o;
            if(!name.equals(gtc.name))
                return false;
            for(int i=0 ; i<args.length ; i++)
                if(args[i] != gtc.args[i])
                    return false;
            return true;
        } else {
            return false;
        }
    }
}
