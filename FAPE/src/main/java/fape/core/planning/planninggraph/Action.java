package fape.core.planning.planninggraph;

import fape.exceptions.FAPEException;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractTemporalStatement;
import planstack.anml.model.abs.statements.*;
import planstack.anml.model.concrete.statements.Persistence;
import planstack.anml.model.concrete.statements.Statement;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Action {

    final AbstractAction act;
    final List<String> params;
    final Map<LVarRef, String> argMap;
    final LinkedList<Fluent> pre = new LinkedList<>();
    final LinkedList<Fluent> add = new LinkedList<>();
    final LinkedList<Fluent> del = new LinkedList<>();

    public Action(AbstractAction act, List<String> params) {
        this.act = act;
        this.params = new LinkedList<>(params);
        argMap = new HashMap<>(params.size());
        for(int i=0 ; i<params.size() ; i++) {
            argMap.put(act.args().apply(i), params.get(i));
        }

        for(AbstractTemporalStatement ts : act.jTemporalStatements()) {
            Fluent precondition = statementToPrecondition(ts.statement());
            if(precondition != null) {
                pre.add(precondition);
            }
            Fluent deletion = statementToDeletion(ts.statement());
            if(precondition != null) {
                del.add(deletion);
            }
            Fluent addition = statementToAddition(ts.statement());
            if(precondition != null) {
                add.add(addition);
            }
        }
    }

    protected Fluent statementToPrecondition(AbstractStatement s) {
        if(!(s instanceof AbstractPersistence || s instanceof AbstractTransition))
            return null;

        LinkedList<String> args = new LinkedList<>();
        for(LVarRef arg : s.sv().jArgs()) {
            args.add(argMap.get(arg));
        }
        if(s instanceof AbstractPersistence) {
            return new Fluent(s.sv().func(), args, argMap.get(((AbstractPersistence) s).value()));
        } else if(s instanceof AbstractTransition) {
            return new Fluent(s.sv().func(), args, argMap.get(((AbstractTransition) s).from()));
        } else {
            throw new FAPEException("Error: should be unreachable.");
        }
    }

    protected Fluent statementToAddition(AbstractStatement s) {
        if(!(s instanceof AbstractAssignment || s instanceof AbstractTransition))
            return null;

        LinkedList<String> args = new LinkedList<>();
        for(LVarRef arg : s.sv().jArgs()) {
            args.add(argMap.get(arg));
        }
        if(s instanceof AbstractAssignment) {
            return new Fluent(s.sv().func(), args, argMap.get(((AbstractAssignment) s).value()));
        } else if(s instanceof AbstractTransition) {
            return new Fluent(s.sv().func(), args, argMap.get(((AbstractTransition) s).to()));
        } else {
            throw new FAPEException("Error: should be unreachable.");
        }
    }

    protected Fluent statementToDeletion(AbstractStatement s) {
        if(!(s instanceof AbstractAssignment || s instanceof AbstractTransition))
            return null;

        LinkedList<String> args = new LinkedList<>();
        for(LVarRef arg : s.sv().jArgs()) {
            args.add(argMap.get(arg));
        }
        if(s instanceof AbstractAssignment) {
            throw new FAPEException("Error: creating delete list from assignment is not supported");
        } else if(s instanceof AbstractTransition) {
            return new Fluent(s.sv().func(), args, argMap.get(((AbstractTransition) s).from()));
        } else {
            throw new FAPEException("Error: should be unreachable.");
        }
    }
}
