package fape.core.planning.states;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StateWrapper {

    public StateWrapper(StateWrapper parent) {
        this.parent = parent;
        this.stronglyReferencedState = null;
        this.mID = State.idCounter++;
    }
    public StateWrapper(State initialState) {
        this.parent = null;
        this.stronglyReferencedState = initialState;
        this.mID = initialState.mID;
    }

    final int mID;



    private final State stronglyReferencedState;
    private Reference<State> state = null;
    private final StateWrapper parent;

    /** Operations to apply to the parent's state to get a complete state. */
    List<Consumer<State>> operations = new ArrayList<>();

    /**
     * Index of the the next operation to apply to state (all operations below this
     * index were already applied
     */
    int nextOperation = 0;

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

    private State getBaseState() {
        if(stronglyReferencedState != null) {
            assert state == null;
            return stronglyReferencedState;
        } else if(state != null && state.get() != null) {
            return state.get();
        } else {
            assert parent != null;
            State st = parent.getState().cc(mID);
            nextOperation = 0;
            state = new SoftReference<>(st);
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

    public void addOperation(Consumer<State> operation) {
        operations.add(operation);
    }

    public int getID() {
        if(state != null && state.get() != null)
            assert state.get().mID == this.mID;
        return this.mID;
    }
}
