package fape.core.planning.preprocessing;

import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import lombok.Value;
import planstack.anml.model.*;
import planstack.anml.model.Function;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractTask;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.concrete.InstanceRef;
import planstack.anml.model.concrete.Task;
import planstack.anml.model.concrete.VarRef;
import planstack.constraints.bindings.Domain;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HierarchicalEffects {

    @Value public class StatementPointer {
        final AbstractAction act;
        final LStatementRef ref;
        final boolean isTransition;
    }

    @Value
    private class Effect {
        @Value class Fluent {
            final Function func;
            final List<VarPlaceHolder> args;
            final VarPlaceHolder value;
        }

        final int delayFromStart;
        final int delayToEnd;
        final Fluent f;
        final List<StatementPointer> origin;

        public Effect(int delayFromStart, int delayToEnd,
                      Function func, List<VarPlaceHolder> args, VarPlaceHolder value,
                      List<StatementPointer> origin) {
            this.delayFromStart = delayFromStart;
            this.delayToEnd = delayToEnd;
            this.f = new Fluent(func, args, value);
            this.origin = origin;
        }

        @Override public String toString() { return "Eff(~>"+delayFromStart+" "+delayToEnd+"<~ "+f.func.name()+f.args+"="+f.value; }

        Effect asEffectOfTask(String t) {
            List<LVarRef> vars = pb.actionsByTask().get(t).get(0).args();
            // function that only keep a variable if it appears in the arguments of the task
            java.util.function.Function<VarPlaceHolder,VarPlaceHolder> trans = (v) -> {
              if(!v.isVar() || vars.contains(v.asVar()))
                  return v;
              else
                  return new TypePlaceHolder(v.asType());
            };
            return new Effect(
                    delayFromStart,
                    delayToEnd,
                    f.func,
                    f.args.stream().map(trans).collect(Collectors.toList()),
                    trans.apply(f.value),
                    origin);
        }

        Effect asEffectBySubtask(AbstractAction a, AbstractTask t) {
            Map<LVarRef,VarPlaceHolder> mapping = new HashMap<>();
            List<LVarRef> taskVars = pb.actionsByTask().get(t.name()).get(0).args();
            for(int i=0 ; i<taskVars.size() ; i++) {
                LVarRef right = t.jArgs().get(i);
                if(a.context().hasGlobalVar(right) && a.context().getGlobalVar(right) instanceof InstanceRef)
                    mapping.put(taskVars.get(i), new InstanceArg((InstanceRef) a.context().getGlobalVar(right)));
                else
                    mapping.put(taskVars.get(i), new LVarArg(t.jArgs().get(i)));
            }
            java.util.function.Function<VarPlaceHolder,VarPlaceHolder> trans = (v) -> {
              if(v.isVar())
                  return mapping.get(v.asVar());
              else
                  return v;
            };
            return new Effect(
                    delayFromStart + a.minDelay(a.start(), t.start()).lb(),
                    delayToEnd + a.minDelay(t.end(), a.end()).lb(),
                    f.func,
                    f.args.stream().map(trans).collect(Collectors.toList()),
                    trans.apply(f.value),
                    origin);
        }

        Effect withoutVars() {
            java.util.function.Function<VarPlaceHolder,VarPlaceHolder> trans = (v) -> {
              if(v.isVar())
                  return new TypePlaceHolder(v.asType());
              else
                  return v;
            };
            return new Effect(
                    delayFromStart,
                    delayToEnd,
                    f.func,
                    f.args.stream().map(trans).collect(Collectors.toList()),
                    trans.apply(f.value),
                    origin);
        }
    }
    private interface VarPlaceHolder {
        boolean isVar();
        default boolean isInstance() { return false; };
        default LVarRef asVar() { throw new UnsupportedOperationException("Not supported yet."); }
        Type asType();
        default InstanceRef asInstance() { throw new UnsupportedOperationException(); }
    }
    @Value private class LVarArg implements VarPlaceHolder {
        final LVarRef var;
        @Override public boolean isVar() { return true; }
        @Override public LVarRef asVar() { return var; }
        @Override public Type asType() { return var.getType(); }
        @Override public String toString() { return var.toString(); }
    }
    @Value private class InstanceArg implements VarPlaceHolder {
        final InstanceRef var;
        @Override public boolean isVar() { return false; }
        @Override public boolean isInstance() { return true; }
        @Override public InstanceRef asInstance() { return var; }
        @Override public Type asType() { return var.getType(); }
        @Override public String toString() { return var.toString(); }
    }
    @Value private class TypePlaceHolder implements VarPlaceHolder {
        final Type type;
        @Override public boolean isVar() { return false; }
        @Override public Type asType() { return type; }
        @Override public String toString() { return type.toString(); }
    }

    private final AnmlProblem pb;
    private Map<String, List<Effect>> tasksEffects = new HashMap<>();
    private Map<AbstractAction, List<Effect>> actionEffects = new HashMap<>();
    private Set<String> pendingTasks = new HashSet<>();
    private Set<String> recTasks = new HashSet<>();

    public HierarchicalEffects(AnmlProblem pb) {
        this.pb = pb;
    }

    private List<Effect> regrouped(List<Effect> effects) {
        Map<Effect.Fluent, List<Effect>> grouped = effects.stream().collect(Collectors.groupingBy(Effect::getF));
        List<Effect> reduced = grouped.keySet().stream().map(f -> {
            int minFromStart = grouped.get(f).stream().map(e -> e.delayFromStart).min(Integer::compare).get();
            int minToEnd = grouped.get(f).stream().map(e -> e.delayToEnd).min(Integer::compare).get();
            List<StatementPointer> origins = grouped.get(f).stream().flatMap(e -> e.getOrigin().stream()).collect(Collectors.toList());
            return new Effect(minFromStart, minToEnd, f.func, f.args, f.value, origins);
        }).collect(Collectors.toList());
        return reduced;
    }

    private List<Effect> effectsOf(AbstractAction a) {
        java.util.function.Function<LVarRef,VarPlaceHolder> asPlaceHolder = (v) -> {
            if(a.context().hasGlobalVar(v) && a.context().getGlobalVar(v) instanceof InstanceRef)
                return new InstanceArg((InstanceRef) a.context().getGlobalVar(v));
            else
                return new LVarArg(v);
        };
        if (!actionEffects.containsKey(a)) {
            List<Effect> directEffects = a.jLogStatements().stream()
                    .filter(AbstractLogStatement::hasEffectAtEnd)
                    .map(s -> new Effect(
                            a.minDelay(a.start(), s.end()).lb(),
                            a.minDelay(s.end(), a.end()).lb(),
                            s.sv().func(),
                            s.sv().jArgs().stream().map(asPlaceHolder).collect(Collectors.toList()),
                            asPlaceHolder.apply(s.effectValue()),
                            Collections.singletonList(new StatementPointer(a, s.id(), s.hasConditionAtStart()))))
                    .collect(Collectors.toList());

            List<Effect> undirEffects = a.jSubTasks().stream()
                    .flatMap(t -> effectsOf(t.name()).stream().map(e -> e.asEffectBySubtask(a, t)))
                    .collect(Collectors.toList());

            List<Effect> effects = new ArrayList<>(directEffects);
            effects.addAll(undirEffects);


            actionEffects.put(a, regrouped(effects));
        }

        return actionEffects.get(a);
    }

    private List<Effect> effectsOf(String task) {
        if(!tasksEffects.containsKey(task)) {
            if(pendingTasks.contains(task)) {
                recTasks.add(task);
                return Collections.emptyList();
            } else {
                pendingTasks.add(task);
                List<Effect> effs = pb.actionsByTask().get(task).stream()
                        .flatMap(a -> effectsOf(a).stream())
                        .map(e -> e.asEffectOfTask(task))
                        .collect(Collectors.toList());
                if (recTasks.contains(task)) {
                    // this task is recursive, remove any vars to make sure to do not miss any effect
                    tasksEffects.put(task, regrouped(effs.stream().map(Effect::withoutVars).collect(Collectors.toList())));
                } else {
                    tasksEffects.put(task, regrouped(effs));
                }
            }
        }
        return tasksEffects.get(task);
    }

    @Value private static class DomainList {
        List<Domain> l;

        /** True if the two lists are compatible: all pairs of domains have a non empty intersection */
        boolean compatible(DomainList dl) {
            assert l.size() == dl.l.size();
            for(int i=0 ; i<l.size() ; i++) {
                if(l.get(i).intersect(dl.l.get(i)).isEmpty())
                    return false;
            }
            return true;
        }

        private static Domain asDomain(VarRef v, State st) { return st.csp.bindings().rawDomain(v); }
        private static Domain asDomain(Type t, State st) { return st.csp.bindings().defaultDomain(t); }
        private static Domain asDomain(VarPlaceHolder v, Map<LVarRef,VarRef> bindings, State st) {
            if(v.isVar()) {
                assert bindings.containsKey(v.asVar());
                return asDomain(bindings.get(v.asVar()), st);
            } else if(v.isInstance()) {
                return asDomain(v.asInstance(), st);
            } else {
                return asDomain(v.asType(), st);
            }
        }
        private static DomainList from(ParameterizedStateVariable sv, VarRef value, State st) {
            return new DomainList(Stream.concat(
                    Stream.of(sv.args()).map(v -> asDomain(v, st)),
                    Stream.of(asDomain(value, st)))
                    .collect(Collectors.toList()));
        }
        private static DomainList from(Effect.Fluent f, Map<LVarRef,VarRef> bindings, State st) {
            return new DomainList(
                    Stream.concat(
                            f.getArgs().stream().map(v -> asDomain(v, bindings, st)),
                            Stream.of(asDomain(f.getValue(), bindings, st)))
                            .collect(Collectors.toList()));
        }
    }

    /**
     * A task can indirectly support an open goal if it can be decomposed in an action
     * producing a statement (i) that can support the open goal (ii) that can be early enough to support it
     */
    public boolean canIndirectlySupport(Timeline og, Task t, State st) {
        DomainList dl = DomainList.from(og.stateVariable, og.getGlobalConsumeValue(), st);
        Map<LVarRef, VarRef> bindings = new HashMap<>();
        for(int i=0 ; i<t.args().size() ; i++) {
            LVarRef localVar = st.pb.actionsByTask().get(t.name()).get(0).args().get(i);
            bindings.put(localVar, t.args().get(i));
        }
        return effectsOf(t.name()).stream()
                .filter(effect -> effect.f.func == og.stateVariable.func())
                .filter(effect -> st.csp.stn().isDelayPossible(t.start(), og.getConsumeTimePoint(), effect.delayFromStart))
                .map(effect -> DomainList.from(effect.f, bindings, st))
                .anyMatch(domainList -> domainList.compatible(dl));
    }
}
