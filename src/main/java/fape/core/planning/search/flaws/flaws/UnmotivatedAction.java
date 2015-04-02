package fape.core.planning.search.flaws.flaws;

import fape.core.planning.planner.APlanner;
import fape.core.planning.preprocessing.ActionDecompositions;
import fape.core.planning.search.flaws.resolvers.ExistingTaskSupporter;
import fape.core.planning.search.flaws.resolvers.MotivatedSupport;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import planstack.anml.model.LActRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.ActionCondition;
import scala.Tuple2;
import scala.Tuple3;

import java.util.LinkedList;
import java.util.List;

/**
 * In the "taskcond" setting, represents an action marked as motivated in the domain
 * which does not support any task condition yet.
 *
 * Possible resolvers are to:
 *  - unify it with any existing task condition
 *  - add an action that provides the task condition and unify it
 *    (this step can also require decomposing the action)
 *  - decompose an existing action to insert the task condition
 */
public class UnmotivatedAction extends Flaw {

    public final Action act;

    public UnmotivatedAction(Action act) {
        assert act.mustBeMotivated();
        this.act = act;
    }

    @Override
    public String toString() {
        return "Unmotivated: "+act.toString();
    }

    @Override
    public List<Resolver> getResolvers(State st, APlanner planner) {
        if(resolvers != null)
            return resolvers;

        resolvers = new LinkedList<>();

        // any task condition unifiable with act
        for(ActionCondition ac : st.getOpenTaskConditions()) {
            boolean unifiable = true;
            if(ac.abs() != act.abs())
                break;
            for(int i=0 ; i<act.args().size() ; i++) {
                unifiable &= st.unifiable(act.args().get(i), ac.args().get(i));
            }
            unifiable &= st.canBeBefore(act.start(), ac.start());
            unifiable &= st.canBeBefore(ac.start(), act.start());
            unifiable &= st.canBeBefore(act.end(), ac.end());
            unifiable &= st.canBeBefore(ac.end(), act.end());
            if(unifiable)
                resolvers.add(new ExistingTaskSupporter(ac, act));
        }

        ActionDecompositions preproc = new ActionDecompositions(st.pb);

        // resolvers: any action we add to the plan and that might provide (through decomposition)
        // a task condition
        for(Tuple3<AbstractAction, Integer, LActRef> insertion : preproc.supporterForMotivatedAction(act)) {
            if(st.isAddable(insertion._1()))
                resolvers.add(new MotivatedSupport(act, insertion._1(), insertion._2(), insertion._3()));
        }

        // resolvers: any action in the plan that can be refined to a task condition
        for(Action a : st.getOpenLeaves()) {
            for (Tuple2<Integer, LActRef> insertion : preproc.supporterForMotivatedAction(a, act)) {
                resolvers.add(new MotivatedSupport(act, a, insertion._1(), insertion._2()));
            }
        }

        return resolvers;
    }
}
