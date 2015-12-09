package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.planner.APlanner;
import fape.exceptions.FAPEException;
import lombok.Value;
import planstack.anml.model.AbstractParameterizedStateVariable;
import planstack.anml.model.Function;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.*;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.abs.time.AbsTP;
import planstack.anml.model.concrete.VarRef;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class DeleteFreeActionsFactory {

    private static final boolean dbg = false;

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

    abstract class FluentTemplate {
        public abstract String funcName();
        public abstract List<LVarRef> args();
    }

    @Value class SVFluentTemplate extends FluentTemplate {
        final AbstractParameterizedStateVariable sv;
        final LVarRef value;
        public String funcName() { return sv.func().name(); }
        public List<LVarRef> args() {
            List<LVarRef> ret = new LinkedList<>(sv.jArgs());
            ret.add(value);
            return ret;
        }
    }
    @Value class DoneFluentTemplate extends FluentTemplate {
        final RActTemplate act;
        public String funcName() { return act.name(); }
        public List<LVarRef> args() { return Arrays.asList(act.abs.allVars()); }// return Arrays.asList(act.args()); }
    }
    @Value class TaskFluentTemplate extends FluentTemplate {
        final String prop;
        final String taskName;
        final List<LVarRef> args;
        public String funcName() { return prop+"--"+taskName; }
        public List<LVarRef> args() { return args; }
    }


    class TempFluentTemplate {
        final FluentTemplate fluent;
        final Time time;
        public TempFluentTemplate(FluentTemplate fluent, int time) {
            this.fluent = fluent;
            this.time = Time.of(time);
        }
        public TempFluentTemplate(FluentTemplate fluent, Time time) {
            this.fluent = fluent;
            this.time = time;
        }
        public String toString() {
            return time+": "+fluent;
        }
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

//        public LVarRef[] args() { return abs.allVars(); }
        public LVarRef[] args() { return abs.args().toArray(new LVarRef[abs.args().size()]); }

        public void addCondition(AbstractParameterizedStateVariable sv, LVarRef var, int time) {
            addCondition(new SVFluentTemplate(sv, var), time);
        }
        public void addCondition(FluentTemplate ft, int time) {
            conditions.add(new TempFluentTemplate(ft, time));
        }

        public void addEffect(AbstractParameterizedStateVariable sv, LVarRef var, int time) {
            addEffect(new SVFluentTemplate(sv, var), time);
        }
        public void addEffect(FluentTemplate ft, int time) {
            effects.add(new TempFluentTemplate(ft, time));
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

    public Collection<RAct> getDeleteFrees(AbstractAction abs, Collection<GAction> grounds, APlanner pl) {
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
            templates.get(start).addCondition(new TaskFluentTemplate("started", task.name(), task.jArgs()), startDelay);
            templates.get(start).addEffect(new TaskFluentTemplate("task", task.name(), task.jArgs()), startDelay);

            AbsTP end = anchorOf(task.end(), abs);
            int endDelay = -relativeTimeOf(task.end(), abs);
            templates.get(end).addCondition(new TaskFluentTemplate("ended", task.name(), task.jArgs()), endDelay+1);
        }

        LVarRef[] actionArgs = abs.args().toArray(new LVarRef[abs.args().size()]);
        // add start of action fluent
        AbsTP startAnchor = anchorOf(abs.start(), abs);
        int startDelay = -relativeTimeOf(abs.start(), abs);
        templates.get(startAnchor).addEffect(new TaskFluentTemplate("started", abs.taskName(), abs.args()), startDelay);

        // add end of action fluent
        AbsTP endAnchor = anchorOf(abs.end(), abs);
        int endDelay = -relativeTimeOf(abs.end(), abs);
        templates.get(endAnchor).addEffect(new TaskFluentTemplate("ended", abs.taskName(), abs.args()), endDelay+1);

        if(abs.motivated()) {
            templates.get(startAnchor).addCondition(new TaskFluentTemplate("task", abs.taskName(), abs.args()), startDelay);
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
                TempFluentTemplate cond = new TempFluentTemplate(f.fluent, newTime);
                leftAct.conditions.add(cond);
            }
            for(TempFluentTemplate f : rightAct.effects) {
                assert f.time instanceof IntTime;
                Time newTime = Time.of(ped.delay(), t -> ((IntTime) f.time).value - leftDelay +rightDelay + (Integer) ped.trans().apply(t));
                TempFluentTemplate eff = new TempFluentTemplate(f.fluent, newTime);
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
        /*
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
        }*/

        // add inter subactions conditions
        for(AbsTP left : templates.keySet()) {
            RActTemplate leftAct = templates.get(left);
            leftAct.addEffect(new DoneFluentTemplate(leftAct), 0);
            for(AbsTP right : templates.keySet()) {
                if(left == right) continue;
                RActTemplate rightAct = templates.get(right);
                int maxDelay = fMaxDelay.apply(left, right);
                leftAct.addCondition(new DoneFluentTemplate(rightAct), maxDelay);
            }
        }


        if(dbg) {
            System.out.println("\n-----------------\n");
            for (RActTemplate at : templates.values()) {
                System.out.println(at);
            }
        }

        List<RAct> relaxedGround = new LinkedList<>();

        for(GAction ground : grounds) {
            for(RActTemplate template : templates.values()) {
                RAct a = RAct.from(template, ground, pl);
                relaxedGround.add(a);
//                System.out.println(a);
            }
        }
        return relaxedGround;
    }
}
