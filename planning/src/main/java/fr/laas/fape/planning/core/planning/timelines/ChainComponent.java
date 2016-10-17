package fr.laas.fape.planning.core.planning.timelines;

import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.anml.model.concrete.statements.Persistence;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A collection of logical statements. This class mainly provides easier operations on a collection of logical statements.
 * A Chain component is immutable and can be shared between different instances.
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

    /** All statements in this chain component. */
    public final LogStatement[] statements;

    /**
     * Creates a new ChainComponent containing a unique statement.
     * @param s Statement to be included in the component
     */
    public ChainComponent(LogStatement s) {
        mID = nextID++;
        statements = new LogStatement[1];
        statements[0] = s;
        change = !(s instanceof Persistence);
    }

    /** Creates a new chain component with the given statements and mID */
    private ChainComponent(LogStatement[] statements, int mID) {
        this.mID = mID;
        assert statements.length > 0;
        this.statements = statements;
        change = !(this.statements[0] instanceof Persistence);
    }

    /** @return Number of statements in this ChainComponent. */
    public final int size() { return statements.length; }

    /** @return First statement in this chain component. */
    public final LogStatement getFirst() { return statements[0]; }

    /**
     * @return One end time point in the chain component (there might be more!)
     */
    public TPRef getSupportTimePoint() {
        assert statements.length == 1 : "There is more than one statement in this chain component.";
        return statements[0].end();
    }

    /**
     * @return One start time point in the chain component (there might be more!)
     */
    public TPRef getConsumeTimePoint() {
        assert statements.length == 1 : "There is more than one statement in this chain component.";
        return statements[0].start();
    }

    public List<TPRef> getStartTimepoints() {
        return Stream.of(statements).map(s -> s.start()).collect(Collectors.toList());
    }

    public List<TPRef> getEndTimepoints() {
        return Stream.of(statements).map(s -> s.end()).collect(Collectors.toList());
    }

    /**
     * Creates a new ChainComponent containing statement in both ChainComponents (this and cc)
     * @param cc
     */
    protected ChainComponent withAll(ChainComponent cc) {
        assert cc.change == this.change : "Error: merging transition and persistence events in the same chain component.";
        LogStatement[] newContents = Arrays.copyOf(statements, statements.length + cc.statements.length);
        for(int i=0 ; i<cc.statements.length ; i++) {
            newContents[i+ statements.length] = cc.statements[i];
        }
        return new ChainComponent(newContents, this.mID);
    }

    protected ChainComponent with(LogStatement s) {
        LogStatement[] newContent = Arrays.copyOf(statements, statements.length+1);
        newContent[statements.length] = s;
        return new ChainComponent(newContent, mID);
    }

    /** Creates a new chain component without statement s */
    protected ChainComponent without(LogStatement s) {
        LogStatement[] newContent = new LogStatement[statements.length-1];
        int i=0;
        for(LogStatement cur : statements) {
            if(!cur.equals(s)) {
                newContent[i] = cur;
                i++;
            }
        }
        return new ChainComponent(newContent, mID);
    }

    /**
     * @return True if the statement is present in the this component.
     */
    public boolean contains(LogStatement s) {
        for(int i=0 ; i< statements.length ; i++)
            if(statements[i].equals(s))
                return true;
        return false;
    }

    /**
     * @return The variable containing the value of the state variable at the end of the component.
     */
    public VarRef getSupportValue() {
        return statements[0].endValue();
    }

    /**
     * @return The variable containing the value of the state variable at the start of the component.
     */
    public VarRef getConsumeValue() {
        return statements[0].startValue();
    }

    /** Returns this chain component (it is immutable) */
    public ChainComponent deepCopy() {
        return this;
    }

    @Override
    public String toString() {
        return Arrays.toString(statements);
    }
}