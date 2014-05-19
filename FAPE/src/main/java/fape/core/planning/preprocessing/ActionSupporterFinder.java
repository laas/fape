package fape.core.planning.preprocessing;

import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.abs.AbstractAction;

import java.util.Collection;

public interface ActionSupporterFinder {

    /**
     * Finds which actions containing a statement that can be used as an enabler for the temporal database.
     *
     * @param db DB that needs enablers
     * @return Actions containing at least one statement that might enable the database.
     */
    public Collection<AbstractAction> getActionsSupporting(State st, TemporalDatabase db);
}
