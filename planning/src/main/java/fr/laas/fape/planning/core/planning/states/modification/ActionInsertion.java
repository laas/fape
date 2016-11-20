package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.Factory;
import fr.laas.fape.planning.core.planning.states.State;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;

@Value
public class ActionInsertion implements StateModification {

    final Action action;

    @Override
    public void apply(State st, boolean isFastForwarding) {
        st.insert(action);
    }

    @Override
    public Collection<Object> involvedObjects() {
        // TODO add objects in the action's chronicle
        return Collections.singletonList(action);
    }
}
