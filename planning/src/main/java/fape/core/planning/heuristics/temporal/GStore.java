package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fr.laas.fape.structures.IRStorage;
import planstack.anml.model.abs.time.AbsTP;
import planstack.anml.model.concrete.InstanceRef;

import java.util.Arrays;
import java.util.List;

public class GStore extends IRStorage {

    public TempFluent.Fluent getFluent(String funcName, List<InstanceRef> argsAndValues) {
        return (TempFluent.Fluent) this.get(TempFluent.Fluent.class, Arrays.asList(funcName, argsAndValues));
    }

    public RAct getRAct(GAction act, AbsTP tp, List<TempFluent> conditions, List<TempFluent> effects) {
        return (RAct) this.get(RAct.class, Arrays.asList(act, tp, conditions, effects));
    }
}
