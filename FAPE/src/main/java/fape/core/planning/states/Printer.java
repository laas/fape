package fape.core.planning.states;

import fape.core.planning.search.Threat;
import fape.core.planning.search.UnboundVariable;
import fape.core.planning.search.UndecomposedAction;
import fape.core.planning.search.UnsupportedTaskCond;
import fape.core.planning.search.resolvers.*;
import fape.core.planning.search.resolvers.Decomposition;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.temporaldatabases.ChainComponent;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import fape.exceptions.FAPEException;
import fape.util.Reporter;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.Assignment;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;
import planstack.anml.model.concrete.statements.Transition;

import java.util.*;

/**
 * Contains functions to produce human-readable string from planning objects.
 * 
 * Most methods are parameterized with the state from which the information can be extracted.
 */
public class Printer {

    public static String stateDependentPrint(State st, Object o) {
        if(o instanceof Action)
            return action(st, (Action) o);
        else if(o instanceof VarRef)
            return variable(st, (VarRef) o);
        else if(o instanceof LogStatement)
            return statement(st, (LogStatement) o);
        else if(o instanceof ParameterizedStateVariable)
            return stateVariable(st, (ParameterizedStateVariable) o);
        else if(o instanceof TemporalDatabase)
            return temporalDatabase(st, (TemporalDatabase) o);
        else if(o instanceof Reporter)
            return ((Reporter) o).Report();
        // Flaws
        else if(o instanceof Threat)
            return "Threat: "+inlineTemporalDatabase(st, ((Threat) o).db1)+" && "+inlineTemporalDatabase(st, ((Threat) o).db2);
        else if(o instanceof UndecomposedAction)
            return "Undecomposed: "+action(st, ((UndecomposedAction) o).action);
        else if(o instanceof UnboundVariable)
            return "Unbound: "+((UnboundVariable) o).var.id()+":"+variable(st, ((UnboundVariable) o).var);
        else if(o instanceof UnsupportedTaskCond) {
            return "UnsuppoertedTaskCondition: "+taskCondition(st, ((UnsupportedTaskCond) o).actCond);
        }

        // Resolvers
        else if(o instanceof TemporalSeparation)
            return "TemporalSeparation: "+inlineTemporalDatabase(st, ((TemporalSeparation) o).first)+" && "
                    +inlineTemporalDatabase(st, ((TemporalSeparation) o).second);
        else if(o instanceof SupportingAction)
            return "SupportingAction: "+((SupportingAction) o).act.name();
        else if(o instanceof SupportingDatabase)
            return "SupportingDatabase: "+inlineTemporalDatabase(st, st.tdb.GetDB(((SupportingDatabase) o).temporalDatabase));
        else if(o instanceof Decomposition)
            return "Decompose: no "+((Decomposition) o).decID;
        else if(o instanceof VarBinding)
            return "VarBinding: "+((VarBinding) o).var.id()+"="+((VarBinding) o).value;
        else if(o instanceof BindingSeparation)
            return "BindingSeparation: "+((BindingSeparation) o).a.id()+"!="+((BindingSeparation) o).b.id();
        else if(o instanceof NewTaskSupporter)
            return "NewTaskSupporter: "+((NewTaskSupporter) o).abs.name();
        else if(o instanceof ExistingTaskSupporter)
            return "ExistingTaskSupporter: "+action(st, ((ExistingTaskSupporter) o).act);

        else
            return o.toString();
    }

    public static String taskNetwork(State st, TaskNetworkManager tn) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tasks: ");
        for(Action a : tn.GetAllActions()) {
            sb.append(action(st, a));
            sb.append("  ");
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String action(State st, Action act) {
        if(act == null)
            return "null";

        String ret = act.name()+"(";
        for(VarRef arg : act.args()) {
            ret += variable(st, arg);
        }
        return ret + "):"+act.id();
    }

    public static String taskCondition(State st, ActionCondition act) {
        if(act == null)
            return "null";

        String ret = act.abs().name()+"(";
        for(VarRef arg : act.args()) {
            ret += variable(st, arg);
        }
        return ret + ")";
    }

    public static String variable(State st, VarRef var) {
        return st.csp.bindings().domainAsString(var);
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

    public static String temporalDatabaseManager(final State st) {
        HashMap<String, List<TemporalDatabase>> groupedDBs = new HashMap<>();
        for(TemporalDatabase tdb : st.getDatabases()) {
            if(!groupedDBs.containsKey(stateVariable(st, tdb.stateVariable))) {
                groupedDBs.put(stateVariable(st, tdb.stateVariable), new LinkedList<TemporalDatabase>());
            }
            groupedDBs.get(stateVariable(st, tdb.stateVariable)).add(tdb);
        }
        for(List<TemporalDatabase> dbs : groupedDBs.values()) {
            Collections.sort(dbs, new Comparator<TemporalDatabase>() {
                @Override
                public int compare(TemporalDatabase db1, TemporalDatabase db2) {
                    return (int) st.getEarliestStartTime(db1.getConsumeTimePoint())
                            - (int) st.getEarliestStartTime(db2.getConsumeTimePoint());
                }
            });
        }
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,List<TemporalDatabase>> entry : groupedDBs.entrySet()) {
            int offset = entry.getKey().length();
            sb.append(entry.getKey());
            boolean first = true;
            for(TemporalDatabase db : entry.getValue()) {
                boolean newDb = true;
                for(ChainComponent cc : db.chain) {
                    for (LogStatement s : cc.contents) {
                        if(!first)
                            for(int i=0;i<offset;i++) sb.append(" ");
                        first = false;
                        if(newDb) sb.append(" | ");
                        else sb.append("   ");
                        newDb = false;
                        sb.append("[");
                        sb.append(st.getEarliestStartTime(s.start()));
                        sb.append(",");
                        sb.append(st.getEarliestStartTime(s.end()));
                        sb.append("] ");
                        if(s instanceof Persistence) {
                            sb.append(" == " + variable(st, s.endValue()));
                        } else if(s instanceof Assignment) {
                            sb.append(" := " + variable(st, s.endValue()));
                        } else if(s instanceof Transition) {
                            sb.append(" == " + variable(st, s.startValue()) +" :-> " +variable(st, s.endValue()));
                        }
                        Action act = st.getActionContaining(s);
                        if(act != null) {
                            sb.append("    From: ");
                            sb.append(action(st, act));
                        }
                        sb.append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String temporalDatabase(State st, TemporalDatabase db) {
        StringBuilder sb = new StringBuilder();
        sb.append(stateVariable(st, db.stateVariable));
        sb.append("  id:"+db.mID+"\n");
        for(ChainComponent cc : db.chain) {
            for(LogStatement s : cc.contents) {
                sb.append("[");
                sb.append(st.getEarliestStartTime(s.start()));
                sb.append(",");
                sb.append(st.getEarliestStartTime(s.end()));
                sb.append("] ");
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

    public static String inlineTemporalDatabase(State st, TemporalDatabase db) {
        StringBuilder sb = new StringBuilder();
        sb.append(stateVariable(st, db.stateVariable));
        sb.append(":"+db.mID+"  ");
        for(ChainComponent cc : db.chain) {
            for(LogStatement s : cc.contents) {
                sb.append("[");
                sb.append(st.getEarliestStartTime(s.start()));
                sb.append(",");
                sb.append(st.getEarliestStartTime(s.end()));
                sb.append("] ");
                sb.append(statement(st, s));
                sb.append(" ");
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
        // TODO
        throw new FAPEException("Find a new way map timepoint with IDs.");/*
        for(Map.Entry<TPRef,Integer> entry : st.csp.stn().ids.entrySet()) {
            if(entry.getValue().equals(stnId))
                return entry.getKey();
        }
        return null;*/
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

    public static String constraints(State st) {
        return st.csp.bindings().Report();
    }
}
