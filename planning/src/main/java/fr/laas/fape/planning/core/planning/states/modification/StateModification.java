package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.planning.core.planning.states.State;

import java.util.Collection;

public interface StateModification {

    void apply(State st, boolean isFastForwarding);

    Collection<Object> involvedObjects();
}
