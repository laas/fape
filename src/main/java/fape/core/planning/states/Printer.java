package fape.core.planning.states;

import fape.core.planning.search.flaws.flaws.*;
import fape.core.planning.search.flaws.resolvers.*;
import fape.core.planning.search.flaws.resolvers.DecomposeAction;
import fape.core.planning.tasknetworks.TaskNetworkManager;
import fape.core.planning.timelines.ChainComponent;
import fape.core.planning.timelines.Timeline;
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

    public static String p(State st, Object o) { return stateDependentPrint(st, o); }

    public static String stateDependentPrint(State st, Object o) {
        if(o instanceof Action)
            return action(st, (Action) o);
        else if(o instanceof VarRef)
            return variable(st, (VarRef) o);
        else if(o instanceof LogStatement)
            return statement(st, (LogStatement) o);
        else if(o instanceof ParameterizedStateVariable)
            return stateVariable(st, (ParameterizedStateVariable) o);
        else if(o instanceof Timeline)
            return temporalDatabase(st, (Timeline) o);
        else if(o instanceof Reporter)
            return ((Reporter) o).report();
        // Flaws
        else if(o instanceof Threat)
            return "Threat: "+inlineTemporalDatabase(st, ((Threat) o).db1)+" && "+inlineTemporalDatabase(st, ((Threat) o).db2);
        else if(o instanceof UndecomposedAction)
            return "Undecomposed: "+action(st, ((UndecomposedAction) o).action);
        else if(o instanceof UnboundVariable)
            return "Unbound: "+((UnboundVariable) o).var.id()+":"+variable(st, ((UnboundVariable) o).var);
        else if(o instanceof UnsupportedTaskCond)
            return "UnsupportedTaskCondition: "+taskCondition(st, ((UnsupportedTaskCond) o).actCond);
        else if(o instanceof UnsupportedTimeline)
            return "Unsupported: "+inlineTemporalDatabase(st, ((UnsupportedTimeline) o).consumer);
        else if(o instanceof UnmotivatedAction)
            return "Unmotivated: "+action(st, ((UnmotivatedAction) o).act);

        // Resolvers
        else if(o instanceof TemporalSeparation)
            return "TemporalSeparation: "+inlineTemporalDatabase(st, ((TemporalSeparation) o).firstDbID)+" && "
                    +inlineTemporalDatabase(st, ((TemporalSeparation) o).secondDbID);
        else if(o instanceof SupportingTimeline)
            return "SupportingDatabase: "+inlineTemporalDatabase(st, st.tdb.getTimeline(((SupportingTimeline) o).supporterID));
        else if(o instanceof DecomposeAction)
            return "Decompose: no "+((DecomposeAction) o).decID;
        else if(o instanceof VarBinding)
            return "VarBinding: "+((VarBinding) o).var.id()+"="+((VarBinding) o).value;
        else if(o instanceof BindingSeparation)
            return "BindingSeparation: "+((BindingSeparation) o).a.id()+"!="+((BindingSeparation) o).b.id();
        else if(o instanceof NewTaskSupporter)
            return "NewTaskSupporter: "+((NewTaskSupporter) o).abs.name();
        else if(o instanceof ExistingTaskSupporter)
            return "ExistingTaskSupporter: "+action(st, ((ExistingTaskSupporter) o).act);
        else if(o instanceof SupportingActionDecomposition)
            return "SupportingActionDecomposition: "+action(st, ((SupportingActionDecomposition) o).act)+" dec: "+((SupportingActionDecomposition) o).decID;
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
        ret += "):"+act.id();
        if(st.taskNet.isDecomposed(act))
            ret += "[dec]";
        return ret;
    }

    public static String groundedAction(State st, Action act) {
        String ret = act.name()+"(";
        for(int i=0 ; i<act.args().size() ; i++) {
            VarRef arg = act.args().get(i);
            assert st.domainSizeOf(arg) == 1 : "Action "+action(st, act)+ "is not grounded.";
            ret += st.domainOf(arg).get(0);
            if(i < act.args().size()-1)
                ret += ",";
        }
        return ret + ")";
    }

    public static String actionsInState(final State st) {
        StringBuilder sb = new StringBuilder();
        List<Action> acts = new LinkedList<>(st.getAllActions());
        Collections.sort(acts, new Comparator<Action>() {
            @Override
            public int compare(Action a1, Action a2) {
                return (int) (st.getEarliestStartTime(a1.start()) - st.getEarliestStartTime(a2.start()));
            }
        });

        for(Action a : acts) {
            int start = (int) st.getEarliestStartTime(a.start());
            int earliestEnd = (int) st.getEarliestStartTime(a.end());
            String name = Printer.action(st, a);
            switch (a.status()) {
                case EXECUTED:
                    sb.append(String.format("%s started:%s ended:%s  [EXECUTED]\n", name, start, earliestEnd));
                    break;
                case EXECUTING:
                    if(st.getDurationBounds(a).nonEmpty()) {
                        int min = st.getDurationBounds(a).get()._1();
                        int max = st.getDurationBounds(a).get()._2();
                        sb.append(String.format("%s \t\tstarted: %s\tduration in [%s, %s]  [EXECUTING]\n", name, start, min, max));
                    } else {
                        sb.append(String.format("%s \t\tstarted: %s\tmin-duration: %s  [EXECUTING]\n", name, start, earliestEnd-start));
                    }
                    break;
                case PENDING:
                    if(st.getDurationBounds(a).nonEmpty()) {
                        int min = st.getDurationBounds(a).get()._1();
                        int max = st.getDurationBounds(a).get()._2();
                        sb.append(String.format("%s \t\tearliest-start: %s\tduration in [%s, %s]\n", name, start, min, max));
                    } else {
                        sb.append(String.format("%s \t\tearliest-start: %s\tmin-duration: %s\n", name, start, earliestEnd-start));
                    }
                    break;
                case FAILED:
            }
        }
        return sb.toString();
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

    public static String bindedVariable(State st, VarRef var) {
        assert st.domainSizeOf(var) == 1;
        return st.domainOf(var).get(0);
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

    public static String groundStateVariable(State st, ParameterizedStateVariable sv) {
        String ret = sv.func().name() + "(";
        for(VarRef arg : sv.jArgs()) {
            ret += bindedVariable(st, arg);
        }
        return ret + ")";
    }

    public static String temporalDatabaseManager(final State st) {
        HashMap<String, List<Timeline>> groupedDBs = new HashMap<>();
        for(Timeline tdb : st.getTimelines()) {
            if(!groupedDBs.containsKey(stateVariable(st, tdb.stateVariable))) {
                groupedDBs.put(stateVariable(st, tdb.stateVariable), new LinkedList<Timeline>());
            }
            groupedDBs.get(stateVariable(st, tdb.stateVariable)).add(tdb);
        }
        for(List<Timeline> dbs : groupedDBs.values()) {
            Collections.sort(dbs, new Comparator<Timeline>() {
                @Override
                public int compare(Timeline db1, Timeline db2) {
                    return (int) st.getEarliestStartTime(db1.getConsumeTimePoint())
                            - (int) st.getEarliestStartTime(db2.getConsumeTimePoint());
                }
            });
        }
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,List<Timeline>> entry : groupedDBs.entrySet()) {
            int offset = entry.getKey().length();
            sb.append(entry.getKey());
            boolean first = true;
            for(Timeline db : entry.getValue()) {
                boolean newDb = true;
                for(ChainComponent cc : db.chain) {
                    for (LogStatement s : cc.statements) {
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

    public static String temporalDatabase(State st, Timeline db) {
        StringBuilder sb = new StringBuilder();
        sb.append(stateVariable(st, db.stateVariable));
        sb.append("  id:"+db.mID+"\n");
        for(ChainComponent cc : db.chain) {
            for(LogStatement s : cc.statements) {
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

    public static String inlineTemporalDatabase(State st, int dbID) {
        return inlineTemporalDatabase(st, st.getTimeline(dbID));
    }

    public static String inlineTemporalDatabase(State st, Timeline db) {
        StringBuilder sb = new StringBuilder();
        sb.append(stateVariable(st, db.stateVariable));
        sb.append(":"+db.mID+"  ");
        for(ChainComponent cc : db.chain) {
            for(LogStatement s : cc.statements) {
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

    public static String timepoint(State st, TPRef tp) {
        if(tp == st.pb.start())
            return "Start";
        else if(tp == st.pb.end())
            return "End";
        else if(tp == st.pb.earliestExecution())
            return "Earliest Exec";

        TemporalInterval in = containingInterval(st, tp);
        if(in == null)
            return "unknown: "+tp;
        else
            return tpToString(tp, containingInterval(st, tp));
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

        for(Timeline db : st.tdb.getTimelines()) {
            for(ChainComponent cc : db.chain) {
                for(LogStatement statement : cc.statements) {
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
