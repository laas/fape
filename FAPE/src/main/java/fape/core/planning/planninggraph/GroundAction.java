package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractTemporalStatement;
import planstack.anml.model.abs.statements.*;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.Factory;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Statement;

import java.util.*;

public class GroundAction implements PGNode {

    final public Action act;
    final public List<VarRef> params;
    final Map<LVarRef, VarRef> argMap;
    public final LinkedList<Fluent> pre = new LinkedList<>();
    public final LinkedList<Fluent> add = new LinkedList<>();
    public final LinkedList<Fluent> del = new LinkedList<>();
    final GroundProblem pb;

    public GroundAction(AbstractAction abs, List<VarRef> params, GroundProblem pb) {

        this.pb = pb;
        this.act = Factory.getInstantiatedAction(pb.liftedPb, abs, params);
        this.params = new LinkedList<>(params);
        argMap = new HashMap<>(params.size());
        for(int i=0 ; i<params.size() ; i++) {
            argMap.put(act.abs().args().get(i), params.get(i));
        }

        for(LogStatement s : act.logStatements()) {
            Fluent precondition = statementToPrecondition(s);
            if(precondition != null) {
                pre.add(precondition);
            }
            for(Fluent f : statementToDeletions(s)) {
                del.add(f);
            }
            Fluent addition = statementToAddition(s);
            if(addition != null) {
                add.add(addition);
            }
        }
    }

    @Override
    public String toString() {
        return act.name() + params.toString();
    }

    /**
     * TODO: extend to look for two preconditions on the same state variable
     * @return True if no item in addition appears in deletion. False otherwise.
     */
    public boolean isValid() {
        Set<Fluent> inter = new HashSet<>(add);
        inter.retainAll(del);
        return inter.isEmpty();
    }

    protected Fluent statementToPrecondition(LogStatement s) {
        return pb.statementToPrecondition(s, argMap);
    }

    protected Fluent statementToAddition(LogStatement s) {
        return pb.statementToAddition(s, argMap);
    }

    protected Collection<Fluent> statementToDeletions(LogStatement s) {
        return pb.statementToDeletions(s, argMap);
    }
}
