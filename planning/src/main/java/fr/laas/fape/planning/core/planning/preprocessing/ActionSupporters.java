package fr.laas.fape.planning.core.planning.preprocessing;

import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.anml.model.Function;
import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.abs.statements.AbstractLogStatement;
import fr.laas.fape.anml.model.abs.statements.AbstractPersistence;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class aims at analysing a problem to identify actions that might enablers for a
 * particular TemporalDatabase.
 *
 * In the current state, analyse is repeated on every method invocation.
 * An action is considered relevant iff it produce a change on the same function (which might be greatly
 * improved by looking at the parameters types).
 */
public class ActionSupporters implements ActionSupporterFinder {

    final AnmlProblem pb;

    public ActionSupporters(AnmlProblem pb) {
        this.pb = pb;
    }

    /**
     * Finds which actions containing a statement that can be used as an enabler for the temporal database.
     *
     * In the current state, it is limited to identifying Assignment and Transition statements on the same function
     * as the TemporalDatabase's.
     * @param db DB that needs enablers
     * @return Actions containing at least one statement that might enable the database.
     */
    public Collection<SupportingAction> getActionsSupporting(State st, Timeline db) {
        assert db.isConsumer() : "Error: this database doesn't need support: "+db;

        return getActionsSupporting(db.stateVariable.func());

    }

    /**
     * Finds which actions contain at least one assignment or transition statement on the function f.
     * @param f Function to look for.
     * @return Actions containing at least one statement inducing a change on the function.
     */
    public Collection<SupportingAction> getActionsSupporting(Function f) {
        Set<SupportingAction> ret = new HashSet<>();

        for(AbstractAction act : pb.abstractActions()) {
            for (AbstractLogStatement s : act.jLogStatements()) {
                if (s.sv().func() == f && !(s instanceof AbstractPersistence)) {
                    ret.add(new SupportingAction(act, s.id()));
                }
            }
        }
        return ret;
    }

}
