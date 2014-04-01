package fape.core.planning.planninggraph;

import java.util.HashSet;
import java.util.Set;

public class GroundState {

    public final Set<Fluent> fluents = new HashSet<>();

    public boolean applicable(GroundAction act) {
        return this.fluents.containsAll(act.pre);
    }

    public GroundState apply(GroundAction act) {
        assert applicable(act);

        GroundState ret = new GroundState();
        ret.fluents.addAll(this.fluents);
        ret.fluents.removeAll(act.del);
        ret.fluents.addAll(act.add);
        return ret;
    }
}
