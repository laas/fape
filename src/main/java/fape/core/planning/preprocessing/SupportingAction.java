package fape.core.planning.preprocessing;


import planstack.anml.model.abs.AbstractAction;

/**
 * An action an might support an open goal.
 *
 * The action might be given a decomposition to apply
 */
public class SupportingAction {
    public int decID;
    public final AbstractAction absAct;

    public SupportingAction(AbstractAction aa, int decID) {
        this.absAct = aa;
        this.decID = decID;
    }
    public SupportingAction(AbstractAction aa) {
        this.absAct = aa;
        this.decID = -1;
    }

    public boolean mustBeDecomposed() { return decID != -1; }
    public int getDecID() { return decID; }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SupportingAction) {
            return ((SupportingAction) o).absAct.equals(this.absAct) && ((SupportingAction) o).decID == this.decID;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.absAct.hashCode() + decID;
    }
}