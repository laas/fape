package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import fape.exceptions.FAPEException;
import lombok.Value;
import planstack.anml.model.AbstractParameterizedStateVariable;
import planstack.anml.model.Function;
import planstack.anml.model.LVarRef;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.abs.*;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.abs.statements.AbstractStatement;
import planstack.anml.model.abs.time.AbsTP;
import planstack.anml.model.abs.time.ContainerStart;
import planstack.anml.model.concrete.VarRef;

import javax.lang.model.type.ArrayType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeleteFreeActionsFactory {

    public static LVarRef truth = new LVarRef("true");

    interface Time {
        static ParameterizedTime of(AbstractParameterizedStateVariable sv, java.util.function.Function<Integer,Integer> trans) {
            return new ParameterizedTime(sv.func(), sv.jArgs(), trans);
        }
        static IntTime of(int value) {
            return new IntTime(value);
        }
    }

    @Value static class IntTime implements Time {
        public final int value;
    }

    @Value static class ParameterizedTime implements Time {
        public final Function f;
        public final List<LVarRef> args;
        public final java.util.function.Function<Integer,Integer> trans;
    }

    class TempFluentTemplate {
        final String funcName;
        final LVarRef[] args;
        final LVarRef value;
        final Time time;
        public TempFluentTemplate(String funcName, LVarRef[] args, LVarRef value, int time) {
            this.funcName = funcName; this.args = args;
            this.value = value; this.time = Time.of(time);
        }
        public TempFluentTemplate(String funcName, LVarRef[] args, LVarRef value, Time time) {
            this.funcName = funcName; this.args = args;
            this.value = value; this.time = time;
        }
        public String toString() {
            return time+": "+funcName+""+Arrays.toString(args)+"="+value;
        }

        public LVarRef[] args() { return args; }
    }

    public class RActTemplate {
        final AbstractAction abs;
        final AbsTP tp;
        public RActTemplate(AbstractAction abs, AbsTP mainTimepoint) {
            this.abs = abs;
            this.tp = mainTimepoint;
        }

        List<TempFluentTemplate> conditions = new LinkedList<>();
        List<TempFluentTemplate> effects = new LinkedList<>();

        public LVarRef[] args() { return abs.args().toArray(new LVarRef[abs.args().size()]); }


        public void addCondition(AbstractParameterizedStateVariable sv, LVarRef var, int time) {
            addCondition(sv.func().name(), sv.jArgs().toArray(new LVarRef[sv.args().size()]), var, time);
        }
        public void addCondition(String funcName, LVarRef[] params, LVarRef value, int time) {
            conditions.add(new TempFluentTemplate(funcName, params, value, time));
        }

        public void addEffect(AbstractParameterizedStateVariable sv, LVarRef var, int time) {
            addEffect(sv.func().name(), sv.jArgs().toArray(new LVarRef[sv.args().size()]), var, time);
        }
        public void addEffect(String funcName, LVarRef[] params, LVarRef value, int time) {
            effects.add(new TempFluentTemplate(funcName, params, value, time));
        }

        public String name() { return abs.name()+"--"+tp+Arrays.toString(args()); }

        public String toString() {
            String s = name() +"\n";
            s += "  conditions:\n";
            for(TempFluentTemplate c : conditions)
                s += "    "+c+"\n";
            s += "  effects:\n";
            for(TempFluentTemplate c : effects)
                s += "    "+c+"\n";
            return s;
        }
    }

    private int relativeTimeOf(AbsTP tp, AbstractAction act) {
        if(act.flexibleTimepoints().contains(tp))
            return 0;
        else {
            for(AbstractAction.AnchoredTimepoint anchored : act.anchoredTimepoints()) {
                if(anchored.timepoint().equals(tp))
                    return anchored.delay();
            }
        }
        throw new FAPEException("Unable to find the timepoint: "+tp);
    }

    private AbsTP anchorOf(AbsTP tp, AbstractAction act) {
        if(act.flexibleTimepoints().contains(tp))
            return tp;
        else {
            for(AbstractAction.AnchoredTimepoint anchored : act.anchoredTimepoints()) {
                if(anchored.timepoint().equals(tp))
                    return anchored.anchor();
            }
        }
        throw new FAPEException("Unable to find the timepoint: "+tp);
    }

    public List<RAct> getDeleteFrees(AbstractAction abs, Collection<GAction> grounds, GroundProblem pb) {
        Map<AbsTP, RActTemplate> templates = new HashMap<>();
        for(AbsTP tp : abs.flexibleTimepoints()) {
            templates.put(tp, new RActTemplate(abs, tp));
        }

        for(AbstractLogStatement s : abs.jLogStatements()) {
            if(s.hasConditionAtStart()) {
                AbsTP main = anchorOf(s.start(), abs);
                int delay = -relativeTimeOf(s.start(), abs);
                templates.get(main).addCondition(s.sv(), s.conditionValue(), delay);
            }
            if(s.hasEffectAfterEnd()) {
                AbsTP main = anchorOf(s.end(), abs);
                int delay = -relativeTimeOf(s.end(), abs);
                templates.get(main).addEffect(s.sv(), s.effectValue(), delay);
            }
        }

        // add subtasks statements
        for(AbstractTask task : abs.jSubTasks()) {
            LVarRef[] taskArgs = task.jArgs().toArray(new LVarRef[task.jArgs().size()]);
            AbsTP start = anchorOf(task.start(), abs);
            int startDelay = -relativeTimeOf(task.start(), abs);
            templates.get(start).addCondition("started-" + task.name(), taskArgs, truth, startDelay);
            templates.get(start).addEffect("task-" + task.name(), taskArgs, truth, startDelay);

            AbsTP end = anchorOf(task.end(), abs);
            int endDelay = -relativeTimeOf(task.end(), abs);
            templates.get(end).addCondition("ended-"+task.name(), taskArgs, truth, endDelay+1);
        }

        LVarRef[] actionArgs = abs.args().toArray(new LVarRef[abs.args().size()]);
        // add start of action fluent
        AbsTP startAnchor = anchorOf(abs.start(), abs);
        int startDelay = -relativeTimeOf(abs.start(), abs);
        templates.get(startAnchor).addEffect("started-" + abs.taskName(), actionArgs, truth, startDelay);

        // add end of action fluent
        AbsTP endAnchor = anchorOf(abs.end(), abs);
        int endDelay = -relativeTimeOf(abs.end(), abs);
        templates.get(endAnchor).addEffect("ended-"+abs.taskName(), actionArgs, truth, endDelay+1);

        if(abs.motivated()) {
            templates.get(startAnchor).addCondition("task-"+abs.taskName(), actionArgs, truth, startDelay);
        }

        List<AbstractParameterizedExactDelay> pmds = abs.jConstraints().stream()
                .filter(ac -> ac instanceof AbstractParameterizedExactDelay)
                .map(ac -> (AbstractParameterizedExactDelay) ac).collect(Collectors.toList());

        // merge actions that are binded by a parameterized exact duration
        for(AbstractParameterizedExactDelay ped : pmds) {
            AbsTP leftAnchor = anchorOf(ped.from(), abs);
            AbsTP rightAnchor = anchorOf(ped.to(), abs);
            if(leftAnchor.equals(rightAnchor))
                continue; // nothing to merge, they are already together
            int leftDelay = -relativeTimeOf(ped.from(), abs);
            int rightDelay = -relativeTimeOf(ped.to(), abs);
            // merge right "action" into left "action"
            RActTemplate leftAct = templates.get(leftAnchor);
            RActTemplate rightAct = templates.get(rightAnchor);
            for(TempFluentTemplate f : rightAct.conditions) {
                assert f.time instanceof IntTime;
                Time newTime = Time.of(ped.delay(), t -> ((IntTime) f.time).value - leftDelay +rightDelay + (Integer) ped.trans().apply(t));
                TempFluentTemplate cond = new TempFluentTemplate(f.funcName, f.args, f.value, newTime);
                leftAct.conditions.add(cond);
            }
            for(TempFluentTemplate f : rightAct.effects) {
                assert f.time instanceof IntTime;
                Time newTime = Time.of(ped.delay(), t -> ((IntTime) f.time).value - leftDelay +rightDelay + (Integer) ped.trans().apply(t));
                TempFluentTemplate eff = new TempFluentTemplate(f.funcName, f.args, f.value, newTime);
                leftAct.effects.add(eff);
            }

            // remove right action as it was merge into left
            templates.remove(rightAnchor);
        }

        BiFunction<AbsTP, AbsTP, Integer> fMinDelay = (x, y) -> {
            return abs.jConstraints().stream()
                    .filter(c -> c instanceof AbstractMinDelay)
                    .map(c -> (AbstractMinDelay) c)
                    .filter(c -> c.from().equals(x) && c.to().equals(y))
                    .map(c -> c.minDelay())
                    .max(Integer::compare)
                    .orElse(-9999999);
        };
        BiFunction<AbsTP, AbsTP, Integer> fMaxDelay = (x, y) -> {
            return abs.jConstraints().stream()
                    .filter(c -> c instanceof AbstractMinDelay)
                    .map(c -> (AbstractMinDelay) c)
                    .filter(c -> c.from().equals(y) && c.to().equals(x))
                    .map(c -> -c.minDelay())
                    .min(Integer::compare)
                    .orElse(9999999);
        };

        // merge all subactions with no conditions into others
        for(AbsTP left : new HashSet<AbsTP>(templates.keySet())) {
            RActTemplate leftAct = templates.get(left);
            if(!leftAct.conditions.isEmpty())
                continue;
            for(AbsTP right : templates.keySet()) {
                if(left == right) continue;
                RActTemplate rightAct = templates.get(right);
                int minDelay = fMinDelay.apply(right, left);
                for(TempFluentTemplate f : leftAct.effects) {
                    assert f.time instanceof IntTime;
                    Time newTime = Time.of(minDelay + ((IntTime) f.time).value);
                    TempFluentTemplate eff = new TempFluentTemplate(f.funcName, f.args, f.value, newTime);
                    rightAct.effects.add(eff);
                }
            }
            templates.remove(left);
        }

        // add inter subactions conditions
        for(AbsTP left : templates.keySet()) {
            RActTemplate leftAct = templates.get(left);
            leftAct.addEffect("done__" + leftAct.name(), leftAct.args(), truth, 0);
            for(AbsTP right : templates.keySet()) {
                if(left == right) continue;
                RActTemplate rightAct = templates.get(right);
                int maxDelay = fMaxDelay.apply(left, right);
                leftAct.addCondition("done__"+rightAct.name(), rightAct.args(), truth, maxDelay);
//                if(abs.name().equals("m-TruckTransport2")) {
//                    System.out.println(left+"  "+right);
//                    System.out.println("");
//                }
            }
        }



        System.out.println("\n-----------------\n");
        for(RActTemplate at : templates.values()) {
            System.out.println(at);
        }

        for(GAction ground : grounds) {
            for(RActTemplate template : templates.values()) {
               System.out.println(RAct.from(template, ground, pb));
            }
        }
        return null;
    }
}
