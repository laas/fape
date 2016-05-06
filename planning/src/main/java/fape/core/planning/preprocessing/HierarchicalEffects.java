package fape.core.planning.preprocessing;

import lombok.Value;
import planstack.anml.model.*;
import planstack.anml.model.Function;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractTask;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.concrete.InstanceRef;
import planstack.structures.Pair;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class HierarchicalEffects {

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

        public Effect(int delayFromStart, int delayToEnd, Function func, List<VarPlaceHolder> args, VarPlaceHolder value) {
            this.delayFromStart = delayFromStart;
            this.delayToEnd = delayToEnd;
            this.f = new Fluent(func, args, value);
        }

        @Override public String toString() { return "Eff(~>"+delayFromStart+" "+delayToEnd+"<~ "+f.func.name()+f.args+"="+f.value; }

        public Effect asEffectOfTask(String t) {
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
                    trans.apply(f.value));
        }

        public Effect asEffectBySubtask(AbstractAction a, AbstractTask t) {
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
                    trans.apply(f.value));
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
                    trans.apply(f.value));
        }
    }
    private interface VarPlaceHolder {
        boolean isVar();
        LVarRef asVar();
        Type asType();
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
        @Override public LVarRef asVar() { throw new UnsupportedOperationException("Not supported yet."); }
        @Override public Type asType() { return var.getType(); }
        @Override public String toString() { return var.toString(); }
    }
    @Value private class TypePlaceHolder implements VarPlaceHolder {
        final Type type;
        @Override public boolean isVar() { return false; }
        @Override public LVarRef asVar() { throw new UnsupportedOperationException("Not supported yet."); }
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

        for(String t : pb.actionsByTask().keySet()) {
            effectsOf(t);
        }
        //effectsOf("t-move_both_arms_to_side");
        System.out.println("coucou");
    }

    private List<Effect> regrouped(List<Effect> effects) {
        Map<Effect.Fluent, List<Effect>> grouped = effects.stream().collect(Collectors.groupingBy(Effect::getF));
        List<Effect> reduced = grouped.keySet().stream().map(f -> {
            int minFromStart = grouped.get(f).stream().map(e -> e.delayFromStart).min(Integer::compare).get();
            int minToEnd = grouped.get(f).stream().map(e -> e.delayToEnd).min(Integer::compare).get();
            return new Effect(minFromStart, minToEnd, f.func, f.args, f.value);
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
                            asPlaceHolder.apply(s.effectValue())))
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
                System.out.println("Hier: "+task);
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
}
