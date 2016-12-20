package fr.laas.fape.planning.core.planning.states;

import fr.laas.fape.anml.model.concrete.*;
import fr.laas.fape.anml.model.concrete.statements.Assignment;
import fr.laas.fape.anml.model.concrete.statements.LogStatement;
import fr.laas.fape.anml.model.concrete.statements.Persistence;
import fr.laas.fape.anml.model.concrete.statements.Transition;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.*;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.mutexes.MutexThreat;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.*;
import fr.laas.fape.planning.core.planning.tasknetworks.TaskNetworkManager;
import fr.laas.fape.planning.core.planning.timelines.ChainComponent;
import fr.laas.fape.planning.core.planning.timelines.FluentHolding;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.anml.model.ParameterizedStateVariable;
import fr.laas.fape.planning.util.Reporter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains functions to produce human-readable string from planning objects.
 * 
 * Most methods are parameterized with the state from which the information can be extracted.
 */
public class Printer {

    public static String p(PartialPlan plan, Object o) { return stateDependentPrint(plan, o); }

    public static String stateDependentPrint(PartialPlan plan, Object o) {
        if(o instanceof Action)
            return action(plan, (Action) o);
        else if(o instanceof VarRef)
            return variable(plan, (VarRef) o);
        else if(o instanceof LogStatement)
            return statement(plan, (LogStatement) o);
        else if(o instanceof ParameterizedStateVariable)
            return stateVariable(plan, (ParameterizedStateVariable) o);
        else if(o instanceof Timeline)
            return timeline(plan, (Timeline) o);
        else if(o instanceof Reporter)
            return ((Reporter) o).report();
        else if(o instanceof FluentHolding)
            return fluent(plan, ((FluentHolding) o).getSv(), ((FluentHolding) o).getValue());

        // Flaws
        else if(o instanceof Threat)
            return "Threat: "+ inlineTimeline(plan, ((Threat) o).db1)+" && "+ inlineTimeline(plan, ((Threat) o).db2);
        else if(o instanceof UnboundVariable)
            return "Unbound: "+((UnboundVariable) o).var.id()+":"+variable(plan, ((UnboundVariable) o).var);
        else if(o instanceof UnrefinedTask)
            return "UnsupportedTaskCondition: "+taskCondition(plan, ((UnrefinedTask) o).task);
        else if(o instanceof UnsupportedTimeline)
            return "Unsupported: "+ inlineTimeline(plan, ((UnsupportedTimeline) o).consumer);
        else if(o instanceof UnmotivatedAction)
            return "Unmotivated: "+action(plan, ((UnmotivatedAction) o).act);
        else if(o instanceof MutexThreat)
            return "MutexThreat: "+fluent(plan, ((MutexThreat) o).getCl1().getSv(), ((MutexThreat) o).getCl1().getValue())+" <-> "+
                    fluent(plan, ((MutexThreat) o).getCl2().getSv(), ((MutexThreat) o).getCl2().getValue());

        // Resolvers
        else if(o instanceof TemporalSeparation)
            return "TemporalSeparation: "+ inlineTimeline(plan, ((TemporalSeparation) o).firstDbID)+" && "
                    + inlineTimeline(plan, ((TemporalSeparation) o).secondDbID);
        else if(o instanceof SupportingTimeline)
            return "SupportingDatabase: "+ inlineTimeline(plan, plan.tdb.getTimeline(((SupportingTimeline) o).supporterID));
        else if(o instanceof VarBinding)
            return "VarBinding: "+((VarBinding) o).var.id()+"="+((VarBinding) o).value;
        else if(o instanceof BindingSeparation)
            return "BindingSeparation: "+((BindingSeparation) o).a.id()+"!="+((BindingSeparation) o).b.id();
        else if(o instanceof NewTaskSupporter)
            return "NewTaskSupporter: "+((NewTaskSupporter) o).abs.name();
        else if(o instanceof ExistingTaskSupporter)
            return "ExistingTaskSupporter: "+action(plan, ((ExistingTaskSupporter) o).act);
        else if(o instanceof FutureTaskSupport) {
            return "FutureTaskSupport: "+taskCondition(plan, ((FutureTaskSupport) o).getTask());
        }
        else
            return o.toString();
    }

    public static String taskNetwork(PartialPlan plan, TaskNetworkManager tn) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tasks: ");
        for(Action a : tn.getAllActions()) {
            sb.append(action(plan, a));
            sb.append("  ");
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String action(PartialPlan plan, Action act) {
        if(act == null)
            return "null";

        String ret = act.name()+"(";
        ret += String.join(", ", act.args().stream().map(v -> variable(plan, v)).collect(Collectors.toList()));

        ret += ") (id:"+act.id()+")";
        return ret;
    }

    public static String tableAsString(List<List<String>> table, int separation) {
        if(table.isEmpty())
            return "";
        List<Integer> maxSizes = new ArrayList<>();
        int longestLine = table.stream().map(l -> l.size()).max(Integer::compare).get();
        for(int i=0 ; i< longestLine ; i++) {
            int maxSize = 0;
            for(List<String> line : table)
                if(i < line.size())
                    maxSize = Math.max(maxSize, line.get(i).length());
            maxSizes.add(maxSize);
        }
        StringBuilder sb = new StringBuilder();
        for(List<String> line : table) {
            for(int i=0; i<line.size() ; i++) {
                sb.append(line.get(i));
                for(int j= maxSizes.get(i)-line.get(i).length() +separation ; j>0 ; j--)
                    sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String actionsInPlan(final PartialPlan plan) {
        List<Action> acts = new LinkedList<>(plan.getAllActions());
        Collections.sort(acts, (Action a1, Action a2) ->
                plan.getEarliestStartTime(a1.start()) - plan.getEarliestStartTime(a2.start()));

        List<List<String>> table = new LinkedList<>();

        for(Action a : acts) {
            int start = plan.getEarliestStartTime(a.start());
            int earliestEnd = plan.getEarliestStartTime(a.end());
            String name = Printer.action(plan, a);
            switch (a.status()) {
                case EXECUTED:
                    table.add(Arrays.asList(start+":", name, "started: "+start, "ended: "+earliestEnd+" [EXECUTED]"));
                    break;
                case EXECUTING:
                    if(plan.getDurationBounds(a).nonEmpty()) {
                        int min = plan.getDurationBounds(a).get()._1();
                        int max = plan.getDurationBounds(a).get()._2();
                        table.add(Arrays.asList(start+":", name, "started: "+start, "duration in ["+min+","+max+"] [EXECUTING]"));
                    } else {
                        table.add(Arrays.asList(start+":", name, "started: "+start, "min-duration: "+(earliestEnd-start)+" [EXECUTING]"));
                    }
                    break;
                case PENDING:
                    if(plan.getDurationBounds(a).nonEmpty()) {
                        int min = plan.getDurationBounds(a).get()._1();
                        int max = plan.getDurationBounds(a).get()._2();
                        table.add(Arrays.asList(start+":", name, "earliest-start: "+start, "duration in ["+min+","+max+"]"));
                    } else {
                        table.add(Arrays.asList(start+":", name, "earliest-start: "+start, "min-duration: "+(earliestEnd-start)));
                    }
                    break;
                case FAILED:
            }
        }
        return tableAsString(table, 3);
    }

    public static String taskCondition(PartialPlan plan, Task task) {
        if(task == null)
            return "null";

        String ret = task.name()+"(";
        for(VarRef arg : task.args()) {
            ret += variable(plan, arg);
        }
        return ret + ")";
    }

    public static String variable(PartialPlan plan, VarRef var) {
        if(plan.domainSizeOf(var) == 1)
            return plan.domainOf(var).get(0);
        else
            return plan.csp.bindings().domainAsString(var);
    }

    public static String boundVariable(PartialPlan plan, VarRef var) {
        assert plan.domainSizeOf(var) == 1;
        return plan.domainOf(var).get(0);
    }

    public static String statement(PartialPlan plan, LogStatement s) {
        String ret = stateVariable(plan, s.sv());
        if(s instanceof Persistence) {
            ret += " == " + variable(plan, s.endValue());
        } else if(s instanceof Assignment) {
            ret += " := " + variable(plan, s.endValue());
        } else if(s instanceof Transition) {
            ret += " == " + variable(plan, s.startValue()) +" :-> " +variable(plan, s.endValue());
        }

        return ret;
    }

    public static String stateVariable(PartialPlan plan, ParameterizedStateVariable sv) {
        String ret = sv.func().name() + "(";
        ret += String.join(", ", Stream.of(sv.args()).map(v -> variable(plan, v)).collect(Collectors.toSet()));
        return ret + ")";
    }

    public static String fluent(PartialPlan plan, ParameterizedStateVariable sv, VarRef value) {
        return stateVariable(plan, sv)+"="+variable(plan, value);
    }

    public static String groundStateVariable(PartialPlan plan, ParameterizedStateVariable sv) {
        String ret = sv.func().name() + "(";
        for(VarRef arg : sv.args()) {
            ret += boundVariable(plan, arg);
        }
        return ret + ")";
    }

    public static String timelines(final PartialPlan plan) {
        HashMap<String, List<Timeline>> groupedDBs = new HashMap<>();
        for(Timeline tdb : plan.getTimelines()) {
            if(!groupedDBs.containsKey(stateVariable(plan, tdb.stateVariable))) {
                groupedDBs.put(stateVariable(plan, tdb.stateVariable), new LinkedList<Timeline>());
            }
            groupedDBs.get(stateVariable(plan, tdb.stateVariable)).add(tdb);
        }
        for(List<Timeline> dbs : groupedDBs.values()) {
            Collections.sort(dbs, (Timeline db1, Timeline db2) ->
                    plan.getEarliestStartTime(db1.getConsumeTimePoint()) - plan.getEarliestStartTime(db2.getConsumeTimePoint()));
        }
        List<List<String>> table = new LinkedList<>();

        for(Map.Entry<String,List<Timeline>> entry : groupedDBs.entrySet()) {
            boolean first = true;
            for(Timeline db : entry.getValue()) {
                boolean newDb = true;
                for(ChainComponent cc : db.chain) {
                    for (LogStatement s : cc.statements) {
                        List<String> line = new LinkedList<>();
                        if(first)
                            line.add(entry.getKey());
                        else
                            line.add("");
                        first = false;
                        if(newDb) line.add("|");
                        else line.add("");
                        newDb = false;
                        line.add("["+plan.getEarliestStartTime(s.start())+","+plan.getEarliestStartTime(s.end())+"]");
                        if(s instanceof Persistence) {
                            line.add("== " + variable(plan, s.endValue()));
                        } else if(s instanceof Assignment) {
                            line.add(":= " + variable(plan, s.endValue()));
                        } else if(s instanceof Transition) {
                            line.add("== " + variable(plan, s.startValue()) +" :-> " +variable(plan, s.endValue()));
                        }
                        Optional<Action> act = plan.getActionContaining(s);
                        if(act.isPresent())
                            line.add("  From: "+action(plan, act.get()));
                        else
                            line.add("  From: problem definition");

                        table.add(line);
                    }
                }
                table.add(Collections.emptyList());
            }
        }

        return tableAsString(table,1);
    }

    public static String timeline(PartialPlan st, Timeline db) {
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

    public static String inlineTimeline(PartialPlan st, int dbID) {
        return inlineTimeline(st, st.getTimeline(dbID));
    }

    public static String inlineTimeline(PartialPlan st, Timeline db) {
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

    public static String timepoint(PartialPlan st, TPRef tp) {
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

    public static TPRef correspondingTimePoint(PartialPlan st, int stnId) {
        // TODO
        throw new FAPEException("Find a new way map timepoint with IDs.");/*
        for(Map.Entry<TPRef,Integer> entry : st.csp.stn().ids.entrySet()) {
            if(entry.getValue().equals(stnId))
                return entry.getKey();
        }
        return null;*/
    }

    public static TemporalInterval containingInterval(PartialPlan st, TPRef tp) {
        for(Action act : st.taskNet.getAllActions()) {
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



    public static String stnId(PartialPlan st, int stnID) {
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

    public static String constraints(PartialPlan st) {
        return st.csp.bindings().report();
    }
}
