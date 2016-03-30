package fape.core.planning.states;

import fape.util.StrongReference;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
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
        state = null;
        this.mID = State.idCounter++;
        this.depth = parent.depth +1;
    }
    public SearchNode(State initialState) {
        this.parent = null;
        state = new StrongReference<>(initialState);
        this.mID = initialState.mID;
        this.depth = 0;
    }

    /** Identifier of the contained state. */
    final int mID;

    /** Depth of this search node */
    final int depth;

    /** A soft, strong or weak reference to a state. */
    private Reference<State> state = null;

    /** Predecessor in hte search tree */
    private final SearchNode parent;

    /** Operations to apply to the parent's state to get a complete state. */
    List<Consumer<State>> operations = new ArrayList<>();

    /**
     * Index of the the next operation to apply to state (all operations below this
     * index were already applied
     */
    int nextOperation = 0;

    public void setExpanded() {
        State s = state.get();
        if(s != null && depth != 0) {
            // switch to a weak reference
            state = new WeakReference<>(s);
        }
    }

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
        if(state != null && state.get() != null) {
            return state.get();
        } else {
            assert depth != 0;
            assert parent != null;
            State st = parent.getStateWithID(mID);
            nextOperation = 0;
            state = new SoftReference<>(st);
            return st;
        }
    }

    public SearchNode getParent() { return parent; }

    private State getStateWithID(int id) {
        if(state != null && state.get() != null) {
            // I already my own state, make sure it is up to date and give it away
            State s = state.get();
            while(nextOperation < operations.size()) {
                operations.get(nextOperation++).accept(s);
            }
            return s.cc(id);
        } else {
            assert depth != 0;
            // ask one from my father and apply my changes
            State s = parent.getStateWithID(id);
            nextOperation = 0;
            while(nextOperation < operations.size()) {
                operations.get(nextOperation++).accept(s);
            }
            if(depth % 5 == 0)
                state = new SoftReference<>(s.cc(mID));
            return s;
        }
    }

    public State getState() {
        State s = getBaseState();
        s.depth = depth;
        while(nextOperation < operations.size()) {
            operations.get(nextOperation++).accept(s);
        }
        return s;
    }

    public int getDepth() { return depth; }

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
