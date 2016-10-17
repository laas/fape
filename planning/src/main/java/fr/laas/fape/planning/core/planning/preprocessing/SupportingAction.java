package fr.laas.fape.planning.core.planning.preprocessing;


import fr.laas.fape.anml.model.LStatementRef;
import fr.laas.fape.anml.model.abs.AbstractAction;

/**
 * An action an might support an open goal.
 *
 * The action might be given a decomposition to apply
 */
public class SupportingAction {
    public final AbstractAction absAct;
    public final LStatementRef statementRef;

    public SupportingAction(AbstractAction aa, LStatementRef statementRef) {
        this.absAct = aa;
        this.statementRef = statementRef;
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SupportingAction) {
            return ((SupportingAction) o).absAct.equals(this.absAct)
                    && ((SupportingAction) o).statementRef.equals(this.statementRef);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.absAct.hashCode()+ statementRef.hashCode();
    }

    @Override
    public String toString() { return absAct.name(); }
}