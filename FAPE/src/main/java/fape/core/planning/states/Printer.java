package fape.core.planning.states;

import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.core.planning.temporaldatabases.TemporalDatabaseManager;
import fape.exceptions.FAPEException;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.TemporalInterval;
import planstack.anml.model.concrete.VarRef;
import planstack.anml.model.concrete.statements.*;

import java.util.Map;

/**
 * Contains functions to produce human-readable string from planning objects.
 * 
 * Most methods are parameterized with the state from which the information can be extracted.
 */
public class Printer {

    public static String action(State st, Action act) {
        if(act == null)
            return "null";

        String ret = act.name()+"(";
        for(VarRef arg : act.args()) {
            ret += variable(st, arg);
        }
        return ret + "):"+act.id();
    }

    public static String variable(State st, VarRef var) {
        return st.conNet.domainOf(var).toString();
    }

    public static String statement(State st, LogStatement s) {
        String ret = stateVariable(st, s.sv());
        if(s instanceof Persistence) {
            ret += " == " + variable(st, s.endValue());
        } else if(s instanceof Assignment) {
            ret += " := " + variable(st, s.endValue());
        } else if(s instanceof Transition) {
            ret += " == " + variable(st, s.startValue()) +" :-> " +variable(st, s.endValue());
        }

        return ret;
    }

    public static String stateVariable(State st, ParameterizedStateVariable sv) {
        String ret = sv.func().name() + "(";
        for(VarRef arg : sv.jArgs()) {
            ret += variable(st, arg);
        }
        return ret + ")";
    }

    public static String temporalDatabaseManager(State st, TemporalDatabaseManager tdbm) {
        StringBuilder sb = new StringBuilder();
        for(TemporalDatabase tdb : tdbm.vars) {
            sb.append(print(st, tdb));
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String print(State st, TemporalDatabase db) {
        StringBuilder sb = new StringBuilder();
        sb.append(stateVariable(st, db.stateVariable));
        sb.append("  id:"+db.mID+"\n");
        for(ChainComponent cc : db.chain) {
            for(LogStatement s : cc.contents) {
                sb.append(statement(st, s));
                Action a  = st.taskNet.getActionContainingStatement(s);
                if(a != null) {
                    sb.append("    \tfrom: ");
                    sb.append(action(st, a));
                }
                sb.append("\n");
            }
            //sb.append("\n");
        }

        return sb.toString();
    }

    private static boolean intervalContains(TPRef tp, TemporalInterval in) {
        return tp == in.start() || tp == in.end();
    }

    private static String tpToString(TPRef tp, TemporalInterval in) {
        if(tp == in.start())
            return "start("+in+")";
        else if(tp == in.end())
            return "end("+in+")";
        else
            throw new FAPEException("Error: time point does not belong to this interval.");
    }

    public static TPRef correspondingTimePoint(State st, int stnId) {
        for(Map.Entry<TPRef,Integer> entry : st.tempoNet.ids.entrySet()) {
            if(entry.getValue().equals(stnId))
                return entry.getKey();
        }
        return null;
    }

    public static TemporalInterval containingInterval(State st, TPRef tp) {
        for(Action act : st.taskNet.GetAllActions()) {
            if(intervalContains(tp, act))
                return act;
        }

        for(TemporalDatabase db : st.tdb.vars) {
            for(ChainComponent cc : db.chain) {
                for(LogStatement statement : cc.contents) {
                    if(intervalContains(tp, statement))
                        return statement;
                }
            }
        }
        return null;
    }



    public static String stnId(State st, int stnID) {
        TPRef tp = correspondingTimePoint(st, stnID);
        if(tp == null)
            return "unknown";

        if(tp == st.pb.start())
            return "start";
        else if(tp == st.pb.earliestExecution())
            return "exec";
        else if(tp == st.pb.end())
            return "end";

        TemporalInterval container = containingInterval(st, tp);

        if(container != null)
            return tpToString(tp, container);
        else
            return "unknown";

    }
}
