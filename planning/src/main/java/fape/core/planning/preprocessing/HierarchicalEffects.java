package fape.core.planning.preprocessing;

import lombok.Value;
import planstack.anml.model.*;
import planstack.anml.model.Function;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.AbstractTask;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.concrete.InstanceRef;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

public class HierarchicalEffects {

    @Value
    private class Effect {
        final int delayFromStart;
        final int delayToEnd;
        final Function func;
        final List<VarPlaceHolder> args;
        final VarPlaceHolder value;
        @Override public String toString() { return "Eff(~>"+delayFromStart+" "+delayToEnd+"<~ "+func.name()+args+"="+value; }

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
                    func,
                    args.stream().map(trans).collect(Collectors.toList()),
                    trans.apply(value));
        }

        public Effect asEffectBySubtask(AbstractAction a, AbstractTask t) {
            Map<LVarRef,LVarRef> mapping = new HashMap<>();
            List<LVarRef> taskVars = pb.actionsByTask().get(t.name()).get(0).args();
            for(int i=0 ; i<taskVars.size() ; i++) {
                mapping.put(taskVars.get(i), t.jArgs().get(i));
            }
            java.util.function.Function<VarPlaceHolder,VarPlaceHolder> trans = (v) -> {
              if(v.isVar())
                  return new LVarArg(mapping.get(v.asVar()));
              else
                  return v;
            };
            return new Effect(
                    delayFromStart + a.minDelay(a.start(), t.start()).lb(),
                    delayToEnd + a.minDelay(t.end(), a.end()).lb(),
                    func,
                    args.stream().map(trans).collect(Collectors.toList()),
                    trans.apply(value));
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
                    func,
                    args.stream().map(trans).collect(Collectors.toList()),
                    trans.apply(value));
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

            actionEffects.put(a, effects);
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
                    tasksEffects.put(task, effs.stream().map(Effect::withoutVars).collect(Collectors.toList()));
                } else {
                    tasksEffects.put(task, effs);
                }
            }
        }
        return tasksEffects.get(task);
    }
}
