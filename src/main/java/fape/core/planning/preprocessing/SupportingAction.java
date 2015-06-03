package fape.core.planning.preprocessing;


import planstack.anml.model.LStatementRef;
import planstack.anml.model.abs.AbstractAction;

/**
 * An action an might support an open goal.
 *
 * The action might be given a decomposition to apply
 */
public class SupportingAction {
    public int decID;
    public final AbstractAction absAct;
    public final LStatementRef statementRef;

    public SupportingAction(AbstractAction aa, int decID, LStatementRef statementRef) {
        this.absAct = aa;
        this.decID = decID;
        this.statementRef = statementRef;
    }
    public SupportingAction(AbstractAction aa, LStatementRef statementRef) {
        this.absAct = aa;
        this.decID = -1;
        this.statementRef = statementRef;
    }

    public boolean mustBeDecomposed() { return decID != -1; }
    public int getDecID() { return decID; }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SupportingAction) {
            return ((SupportingAction) o).absAct.equals(this.absAct)
                    && ((SupportingAction) o).decID == this.decID
                    && ((SupportingAction) o).statementRef.equals(this.statementRef);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.absAct.hashCode() + decID + statementRef.hashCode();
    }

    @Override
    public String toString() { return absAct.name(); }
}