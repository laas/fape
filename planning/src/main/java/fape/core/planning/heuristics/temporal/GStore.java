package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GTask;
import fr.laas.fape.structures.IRStorage;
import planstack.anml.model.abs.time.AbsTP;
import planstack.anml.model.concrete.InstanceRef;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GStore extends IRStorage {

    public TempFluent.DGFluent getFluent(String funcName, List<InstanceRef> argsAndValues) {
        return (TempFluent.DGFluent) this.get(TempFluent.DGFluent.class, Arrays.asList(funcName, argsAndValues));
    }

    public RAct getRAct(GAction act, AbsTP tp, List<TempFluent> conditions, List<TempFluent> effects) {
        return (RAct) this.get(RAct.class, Arrays.asList(act, tp, conditions, effects));
    }

    public DependencyGraph.FactAction getFactAction(List<TempFluent> facts) {
        return (DependencyGraph.FactAction) this.get(DependencyGraph.FactAction.class, Collections.singletonList(facts));
    }

    public GTask getTask(String name, List<InstanceRef> args) {
        return (GTask) this.get(GTask.class, Arrays.asList(name, args));
    }
}
