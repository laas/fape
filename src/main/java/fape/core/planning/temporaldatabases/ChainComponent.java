package fape.core.planning.temporaldatabases;

import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;

import java.util.LinkedList;

/**
 *
 */
public class ChainComponent {

    /**
     * True if this chain component implies a change on the state variable (ie transition or assignment),
     * False otherwise.
     */
    public final boolean change;

    /**
     * Id of the chain component. This ID is shared with any instance cloned from this one.
     */
    public final int mID;

    /** Next mID for newly created ChainComponents. */
    private static int nextID=0;

    /**
     *
     */
    public final LinkedList<LogStatement> contents = new LinkedList<>();

    /**
     * Creates a new ChainComponent containing a unique statement.
     * @param s Statement to be included in the component
     */
    public ChainComponent(LogStatement s) {
        mID = nextID++;
        contents.add(s);
        if (s instanceof Persistence) {
            change = false;
        } else {
            change = true;
        }
    }

    /**
     * Creates a new ChainComponent with the same content.
     * @param toCopy ChainComponent to copy
     */
    public ChainComponent(ChainComponent toCopy) {
        mID = toCopy.mID;
        contents.addAll(toCopy.contents);
        change = toCopy.change;
    }

    /**
     * @return One end time point in the chain component (there might be more!)
     */
    public TPRef getSupportTimePoint() {
        assert contents.size() == 1 : "There is more than one statement in this chain component.";
        return contents.getFirst().end();
    }

    /**
     * @return One start time point in the chain component (there might be more!)
     */
    public TPRef getConsumeTimePoint() {
        assert contents.size() == 1 : "There is more than one statement in this chain component.";
        return contents.getFirst().start();
    }

    /**
     * Add all events of the parameterized chain component to the current chain component.
     * @param cc
     */
    public void Add(ChainComponent cc) {
        assert cc.change == this.change : "Error: merging transition and persistence events in the same chain component.";
        contents.addAll(cc.contents);
    }

    /**
     * @return True if the statement is present in the this component.
     */
    public boolean contains(LogStatement s) {
        return contents.contains(s);
    }

    /**
     * @return The variable containing the value of the state variable at the end of the component.
     */
    public VarRef GetSupportValue() {
        return contents.getFirst().endValue();
    }

    /**
     * @return The variable containing the value of the state variable at the start of the component.
     */
    public VarRef GetConsumeValue() {
        return contents.getFirst().startValue();
    }

    public ChainComponent DeepCopy() {
        return new ChainComponent(this);
    }

    @Override
    public String toString() {
        return contents.toString();
    }
}