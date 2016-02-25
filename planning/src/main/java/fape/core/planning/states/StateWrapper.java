package fape.core.planning.states;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StateWrapper {

    public StateWrapper(StateWrapper parent) {
        this.parent = parent;
        this.mID = State.idCounter++;
    }
    public StateWrapper(State initialState) {
        this.parent = null;
        this.state = initialState;
        this.mID = initialState.mID;
    }

    final int mID;

    List<Consumer<State>> operations = new ArrayList<>();
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

    int nextOperation = 0;
    private State state = null;
    private final StateWrapper parent;

    public State getState() {
        while(nextOperation < operations.size()) {
            if(state == null)
                state = parent.getState().cc(mID);
            operations.get(nextOperation++).accept(state);
        }
        return state;
    }

    public void addOperation(Consumer<State> operation) {
        operations.add(operation);
    }

    public int getID() {
        if(state != null)
            assert state.mID == this.mID;
        return this.mID;
    }
}
