package fape.core.planning.states;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Node of the search tree.
 *
 * A node is a description to build a State :
 *  - a link to the preceding node in the search tree from which a base state can be obtained
 *  - a list of operation to apply to the base state.
 *
 * The State object is cached using soft references. If the JVM runs out of memory, cached states
 * are reclaimed by the garbage collector. The least recently used state is typically reclaimed first.
 *
 * The search node also provides a caching mechanism for heuristic values
 * (to avoid rebuilding a complete state to extract the heuristic value).
 */
public class SearchNode {

    public SearchNode(SearchNode parent) {
        this.parent = parent;
        this.stronglyReferencedState = null;
        this.mID = State.idCounter++;
    }
    public SearchNode(State initialState) {
        this.parent = null;
        this.stronglyReferencedState = initialState;
        this.mID = initialState.mID;
    }

    /** Identifier of the contained state. */
    final int mID;

    /** A strong reference to a state that we cannot afford to forget about.
     * This is used at least for the root of the search tree */
    private final State stronglyReferencedState;

    /** Typically a soft reference to a state. */
    private Reference<State> forgettableState = null;

    /** Predecessor in hte search tree */
    private final SearchNode parent;

    /** Operations to apply to the parent's state to get a complete state. */
    List<Consumer<State>> operations = new ArrayList<>();

    /**
     * Index of the the next operation to apply to state (all operations below this
     * index were already applied
     */
    int nextOperation = 0;

    /** List of 'h', 'g', 'hc' heuristic values */
    List<Float> hs = new ArrayList<>();
    List<Float> gs = new ArrayList<>();
    List<Float> hcs = new ArrayList<>();

    public float getH(int heuristicID) { return hs.get(heuristicID); }
    public boolean isRecordedH(int heuristicID) { return hs.size() > heuristicID && hs.get(heuristicID) != Integer.MIN_VALUE; }
    public void setH(int heuristicID, float value) {
        while(hs.size() <= heuristicID)
            hs.add(Float.MIN_VALUE);
        hs.set(heuristicID, value);
    }
    public float getG(int heuristicID) { return gs.get(heuristicID); }
    public boolean isRecordedG(int heuristicID) { return gs.size() > heuristicID && gs.get(heuristicID) != Integer.MIN_VALUE; }
    public void setG(int heuristicID, float value) {
        while(gs.size() <= heuristicID)
            gs.add(Float.MIN_VALUE);
        gs.set(heuristicID, value);
    }
    public float getHC(int heuristicID) { return hcs.get(heuristicID); }
    public boolean isRecordedHC(int heuristicID) { return hcs.size() > heuristicID && hcs.get(heuristicID) != Integer.MIN_VALUE; }
    public void setHC(int heuristicID, float value) {
        while(hcs.size() <= heuristicID)
            hcs.add(Float.MIN_VALUE);
        hcs.set(heuristicID, value);
    }

    /**
     * Returns the base state from which the complete state can be built.
     */
    private State getBaseState() {
        if(stronglyReferencedState != null) {
            assert forgettableState == null;
            return stronglyReferencedState;
        } else if(forgettableState != null && forgettableState.get() != null) {
            return forgettableState.get();
        } else {
            assert parent != null;
            State st = parent.getState().cc(mID);
            nextOperation = 0;
            forgettableState = new SoftReference<>(st);
            return st;
        }
    }

    public State getState() {
        State s = getBaseState();
        while(nextOperation < operations.size()) {
            operations.get(nextOperation++).accept(s);
        }
        return s;
    }

    /**
     * Appends a new operation necessary to build the complete state.
     */
    public void addOperation(Consumer<State> operation) {
        operations.add(operation);
    }

    public int getID() {
//        if(forgettableState != null && forgettableState.get() != null)
//            assert forgettableState.get().mID == this.mID; // this can cause a failure if the GC is invoked between those two lines
        return this.mID;
    }
}
