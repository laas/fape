package fape.core.planning.planner;

import fape.core.planning.states.State;
import fape.util.Pair;

import java.util.Comparator;

public class AEComparator implements Comparator<State> {
    private final APlanner planner;

    public AEComparator(APlanner planner, float a, float b) {
        this.planner = planner;
        this.A = a;
        this.B = b;
    }

    protected  final float A ;
    protected  final float B ;

    public int compare(State x,State y){
        float f_state = f(x);
        float f_state2 = f(y);

        // comparison (and not difference) is necessary since the input is a float.
        if(f_state > f_state2)
            return 1;
        else if(f_state2 > f_state)
            return -1;
        else
            return 0;
    }

    public float f(State s) {
        return A  * (planner.g(s)) + B * (planner.h(s));
    }
}

