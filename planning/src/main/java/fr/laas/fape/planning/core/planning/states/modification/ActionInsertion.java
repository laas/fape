package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.planning.core.planning.states.State;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Value
public class ActionInsertion implements StateModification {

    public final Action action;

    @Override
    public void apply(State st, boolean isFastForwarding) {
        st.insert(action);
    }

    @Override
    public Collection<Object> involvedObjects() {
        Collection<Object> objs = new ChronicleInsertion(action.chronicle()).involvedObjects();
        objs.addAll(action.jUsedVariables());
        objs.add(action);
        return objs;
    }
}
