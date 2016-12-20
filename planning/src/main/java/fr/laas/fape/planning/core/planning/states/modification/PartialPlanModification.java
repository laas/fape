package fr.laas.fape.planning.core.planning.states.modification;

import fr.laas.fape.planning.core.planning.states.PartialPlan;

import java.util.Collection;

public interface PartialPlanModification {

    void apply(PartialPlan plan, boolean isFastForwarding);

    Collection<Object> involvedObjects();
}
