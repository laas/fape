package fr.laas.fape.planning.core.execution.model;

import fr.laas.fape.anml.model.concrete.ActRef;
import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.LinkedList;
import java.util.List;

/**
 * An atomic action is a representation of an action to be carried out by an actor.
 * Its parameters are ground (refer to instances of the domain) and has a start time and expected duration.
 */
public class AtomicAction {


    /**
     * Creates a new actomic action.
     * @param action The concrete action to serve as a base.
     * @param startTime The start time of the action.
     * @param minDuration The minimal expected duration of the action.
     * @param maxDuration THe maximal expected duration
     * @param st State in which the action appears. It is used to translate global variables to actual problem instances.
     */
    public AtomicAction(Action action, int startTime, int minDuration, int maxDuration, State st) {
        id = action.id();
        name = action.name();
        mStartTime = startTime;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        params = new LinkedList<>();
        for(VarRef arg : action.args()) {
            List<String> possibleValues = new LinkedList<>(st.domainOf(arg));
            assert possibleValues.size() == 1 : "Argument "+arg+" of action "+action+" has more than one possible value.";
            params.add(possibleValues.get(0));
        }
    }

    public AtomicAction(ActRef id, String name, List<String> params, int startTime, int minDuration, int maxDuration) {
        this.id = id;
        this.name = name;
        this.params = params;
        this.mStartTime = startTime;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    public enum EResult{ SUCCESS, FAILURE }

    /**
     * Time at which the action execution has to start.
     */
    public final int mStartTime;

    /**
     * Reference to the concrete action in the plan.
     */
    public final ActRef id;

    /**
     * Minimal expected duration
     */
    public final int minDuration;

    /** Maximal expected duration */
    public final int maxDuration;

    /**
     * Name of the action.
     */
    public final String name;

    /**
     * Parameters of the action in the form of domain instances.
     */
    public final List<String> params;

    /**
     * @return A human readable string representing the action.
     */
    public String GetDescription(){
        String ret = "";
        
        ret += "("+name;
        for(String st:params){
            ret += " "+st;
        }
        ret += ")";        
        return ret;
    }

    @Override
    public String toString() {
        return GetDescription();
    }
}
