package fape.core.planning.preprocessing;

import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.Function;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.AbstractPersistence;
import planstack.anml.model.abs.AbstractTemporalStatement;
import planstack.anml.model.concrete.VarRef;

import java.util.*;
import planstack.anml.model.abs.statements.AbstractConsumeResource;
import planstack.anml.model.abs.statements.AbstractProduceResource;

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
    public Collection<AbstractAction> getActionsSupporting(State st, TemporalDatabase db) {
        assert db.isConsumer() : "Error: this database doesn't need support: "+db;

        return getActionsSupporting(db.stateVariable.func());

    }

    /**
     * Finds which actions contain at least one assignment or transition statement on the function f.
     * @param f Function to look for.
     * @return Actions containing at least one statement inducing a change on the function.
     */
    public Collection<AbstractAction> getActionsSupporting(Function f) {
        Set<AbstractAction> ret = new HashSet<>();

        for(AbstractAction act : pb.abstractActions()) {
            for(AbstractTemporalStatement ts : act.jTemporalStatements()) {
                if(ts.statement().sv().func() == f && !(ts.statement() instanceof AbstractPersistence)) {
                    assert !ret.contains(act) : "Action "+act+" has at least two statements supporting the database.";
                    ret.add(act);
                }
            }
        }
        return ret;
    }

}
