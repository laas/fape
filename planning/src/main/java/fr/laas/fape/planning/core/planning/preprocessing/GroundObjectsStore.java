package fr.laas.fape.planning.core.planning.preprocessing;

import fr.laas.fape.anml.model.Function;
import fr.laas.fape.anml.model.abs.time.AbsTP;
import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.planning.core.planning.grounding.Fluent;
import fr.laas.fape.planning.core.planning.grounding.GAction;
import fr.laas.fape.planning.core.planning.grounding.GStateVariable;
import fr.laas.fape.planning.core.planning.grounding.GTask;
import fr.laas.fape.planning.core.planning.reachability.ElementaryAction;
import fr.laas.fape.planning.core.planning.reachability.ReachabilityGraph;
import fr.laas.fape.planning.core.planning.reachability.TempFluent;
import fr.laas.fape.structures.IRStorage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GroundObjectsStore extends IRStorage {

    public ElementaryAction getRAct(GAction act, AbsTP tp, List<TempFluent> conditions, List<TempFluent> effects) {
        return (ElementaryAction) this.get(ElementaryAction.class, Arrays.asList(act, tp, conditions, effects));
    }

    public ReachabilityGraph.FactAction getFactAction(List<TempFluent> facts) {
        return (ReachabilityGraph.FactAction) this.get(ReachabilityGraph.FactAction.class, Collections.singletonList(facts));
    }

    public GTask getTask(String name, List<InstanceRef> args) {
        return (GTask) this.get(GTask.class, Arrays.asList(name, args));
    }

    public Fluent getFluent(GStateVariable sv, InstanceRef value) {
        return (Fluent) this.get(Fluent.class, Arrays.asList(sv, value));
    }

    public Fluent getFluentByID(int fluentID) {
        return (Fluent) this.get(Fluent.class, fluentID);
    }

    public GStateVariable getGStateVariable(Function f, List<InstanceRef> vars) {
        return (GStateVariable) this.get(GStateVariable.class, Arrays.asList(f, vars));
    }
}
