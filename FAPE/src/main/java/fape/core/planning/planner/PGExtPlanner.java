package fape.core.planning.planner;

import fape.core.planning.constraints.ConservativeConstraintNetwork;
import fape.core.planning.planninggraph.DisjunctiveAction;
import fape.core.planning.planninggraph.DisjunctiveFluent;
import fape.core.planning.planninggraph.GroundAction;
import fape.core.planning.planninggraph.PGPlanner;
import fape.core.planning.states.State;
import fape.exceptions.FAPEException;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.LogStatement;

import java.util.LinkedList;
import java.util.List;

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

    @Override
    public String shortName() { return "rpg_ext"; }

    @Override
    public void causalLinkAdded(State st, LogStatement supporter, LogStatement consumer) {
        Action supportingAction = st.getActionContaining(supporter);

        if(supportingAction != null) {
            DisjunctiveFluent fluent = new DisjunctiveFluent(consumer.sv(), consumer.startValue(), st.conNet, groundPB);
            DisjunctiveAction dAct = pg.enablers(fluent);
            unify(st, supportingAction, dAct);
        }
    }



    //TODO doc
    public void unify(State st, Action act, DisjunctiveAction disAct) {
        ConservativeConstraintNetwork cn = (ConservativeConstraintNetwork) st.conNet;

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
            values.add(valueSeq);
        }
//        cn.addExtensionConstraint(act.args(), values); //TODO: uncomment
    }
}
