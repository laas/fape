package fape.core.planning.planninggraph;

import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.InstanceRef;

import java.util.Collection;

public class GTaskCond {
    public final AbstractAction act;
    public final InstanceRef[] args;

    public GTaskCond(AbstractAction abs, Collection<InstanceRef> args) {
        this.act = abs;
        this.args = new InstanceRef[args.size()];
        assert args.size() == abs.args().size();
        int i=0;
        for(InstanceRef arg : args) {
            this.args[i] = arg;
            i++;
        }
    }

    @Override
    public int hashCode() {
        int ret = act.hashCode();
        for(int i=0 ; i<args.length ; i++) {
            ret += 42^i * args[i].hashCode();
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof GTaskCond) {
            GTaskCond gtc = (GTaskCond) o;
            if(act != gtc.act)
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
