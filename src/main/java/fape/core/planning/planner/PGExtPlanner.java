package fape.core.planning.planner;

import fape.core.planning.planninggraph.DisjunctiveAction;
import fape.core.planning.planninggraph.DisjunctiveFluent;
import fape.core.planning.planninggraph.GroundAction;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.ActRef;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.constraints.stnu.Controllability;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Extension of PGPlanner that add constraint on every causal link whose supporter
 * is provided by an action. It cheks (using RPG) for every ground action
 * supporting the consmer and enforce a n-ary constraint on the action's
 * parameters to make sure they fit at least one of the ground action.
 *
 * This is done by implementing the causalLinkAdded method that is called whenever a causal link is
 * added to the state.
 */
public class PGExtPlanner extends PGPlanner {

    public PGExtPlanner(State initialState, String[] planSelStrategies, String[] flawSelStrategies) {
        super(initialState, planSelStrategies, flawSelStrategies);
    }

    public PGExtPlanner(Controllability controllability, String[] planSelStrategies, String[] flawSelStrategies) {
        super(controllability, planSelStrategies, flawSelStrategies);
    }

    @Override
    public String shortName() { return "rpg_ext"; }

    @Override
    public void causalLinkAdded(State st, LogStatement supporter, LogStatement consumer) {
        Action supportingAction = st.getActionContaining(supporter);

        if(supportingAction != null) {
            DisjunctiveFluent fluent = new DisjunctiveFluent(consumer.sv(), consumer.startValue(), st, groundPB);
            DisjunctiveAction dAct = pg.enablers(fluent);
            unify(st, supportingAction, dAct);
        }
    }

    static int nextConstraint = 0;

    public void unify(State st, Action act, DisjunctiveAction disAct) {
        String constraintID = "rpg_ext" + nextConstraint++;


        LinkedList<List<String>> values = new LinkedList<>();
        for(GroundAction ga : disAct.actions) {
            if(ga.act.abs() != act.abs())
                continue;

            LinkedList<String> valueSeq = new LinkedList<>();
            for(VarRef val : ga.params) {
                if(val instanceof InstanceRef) {
                    valueSeq.add(((InstanceRef) val).instance());
                } else {
                    throw new FAPEException("ERROR: ground param is not an instance");
                }
            }
            st.addValuesToValuesSet(constraintID, valueSeq);
        }

        st.addValuesSetConstraint(act.args(), constraintID);
    }
}
