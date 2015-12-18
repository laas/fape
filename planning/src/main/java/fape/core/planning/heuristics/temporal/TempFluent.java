package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.*;
import fape.exceptions.FAPEException;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.ValueConstructor;
import lombok.Value;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.LVarRef;
import planstack.anml.model.abs.time.AbsTP;
import planstack.anml.model.concrete.InstanceRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value public class TempFluent {

    @Ident(DependencyGraph.Node.class)
    public static abstract class DGFluent extends DependencyGraph.Node {

        public static DGFluent from(DeleteFreeActionsFactory.FluentTemplate template, GAction container, AnmlProblem pb, GStore store) {
            if(template instanceof DeleteFreeActionsFactory.DoneFluentTemplate) {
                return (ActEndFluent) store.get(ActEndFluent.class, Arrays.asList(container, ((DeleteFreeActionsFactory.DoneFluentTemplate) template).act.tp));
            } else if(template instanceof DeleteFreeActionsFactory.SVFluentTemplate) {
                DeleteFreeActionsFactory.SVFluentTemplate ft = (DeleteFreeActionsFactory.SVFluentTemplate) template;
                List<InstanceRef> args = new ArrayList<>();
                for(LVarRef lArg : ft.sv.jArgs())
                    args.add(container.valueOf(lArg, pb));
                GStateVariable sv = store.getGStateVariable(ft.sv.func(), args);
                Fluent f = store.getFluent(sv, container.valueOf(ft.value, pb));
                return (SVFluent) store.get(SVFluent.class, Collections.singletonList(f));
            } else {
                DeleteFreeActionsFactory.TaskFluentTemplate tft = (DeleteFreeActionsFactory.TaskFluentTemplate) template;
                List<InstanceRef> args = new ArrayList<>();
                for(LVarRef lArg : tft.args)
                    args.add(container.valueOf(lArg, pb));
                return (TaskPropFluent) store.get(TaskPropFluent.class, Arrays.asList(tft.prop, store.getTask(tft.taskName, args)));
            }
        }

        public static DGFluent from(fape.core.planning.grounding.Fluent f, GStore store) {
            return (SVFluent) store.get(SVFluent.class, Collections.singletonList(f));
        }
        public static DGFluent from(GTask task, AnmlProblem pb, GStore store) {
            return (DGFluent) store.get(TaskPropFluent.class, Arrays.asList("task", task));
        }
    }

    @Ident(DependencyGraph.Node.class) public static class ActEndFluent extends DGFluent {
        public final GAction act;
        public final AbsTP tp;
        @ValueConstructor @Deprecated
        public ActEndFluent(GAction act, AbsTP tp) { this.act = act; this.tp = tp; }
    }
    @Ident(DependencyGraph.Node.class) public static class SVFluent extends DGFluent {
        public final Fluent fluent;
        @ValueConstructor @Deprecated
        public SVFluent(Fluent f) { this.fluent = f; }

        @Override
        public String toString() { return fluent.toString(); }
    }
    @Ident(DependencyGraph.Node.class) public static class TaskPropFluent extends DGFluent {
        public final String proposition;
        public final GTask task;
        @ValueConstructor @Deprecated
        public TaskPropFluent(String prop, GTask task) { this.proposition = prop; this.task = task; }
    }

    public final int time;
    public final DGFluent fluent;

    @Override public String toString() { return time+": "+fluent; }

    public static TempFluent from(DeleteFreeActionsFactory.TempFluentTemplate template, GAction container, GroundProblem pb, GStore store) {
        int time;
        if(template.time instanceof DeleteFreeActionsFactory.IntTime) {
            time = ((DeleteFreeActionsFactory.IntTime) template.time).getValue();
        } else if(template.time instanceof DeleteFreeActionsFactory.ParameterizedTime) {
            List<InstanceRef> params = ((DeleteFreeActionsFactory.ParameterizedTime) template.time).args.stream()
                    .map(v -> container.valueOf(v, pb.liftedPb))
                    .collect(Collectors.toList());
            time = pb.intInvariants.get(new GroundProblem.IntegerInvariantKey(((DeleteFreeActionsFactory.ParameterizedTime) template.time).f, params));
        } else {
            throw new FAPEException("Unsupported time template: "+template.time);
        }

        DGFluent fluent = DGFluent.from(template.fluent, container, pb.liftedPb, store);

        return new TempFluent(time, fluent);
    }
}
