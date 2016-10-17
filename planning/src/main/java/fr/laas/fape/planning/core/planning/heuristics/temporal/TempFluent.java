package fr.laas.fape.planning.core.planning.heuristics.temporal;

import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.anml.model.abs.time.AbsTP;
import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.planning.core.planning.grounding.*;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.ValueConstructor;
import lombok.Value;
import fr.laas.fape.anml.model.LVarRef;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Value public class TempFluent {

    @Ident(DependencyGraph.Node.class)
    public static abstract class DGFluent extends DependencyGraph.Node {

        public static DGFluent from(DeleteFreeActionsFactory.FluentTemplate template, GAction container, AnmlProblem pb, GStore store) {
            if(template instanceof DeleteFreeActionsFactory.DoneFluentTemplate) {
                return (ActEndFluent) store.get(ActEndFluent.class, Arrays.asList(container, ((DeleteFreeActionsFactory.DoneFluentTemplate) template).act.tp));
            } else if(template instanceof DeleteFreeActionsFactory.SVFluentTemplate) {
                DeleteFreeActionsFactory.SVFluentTemplate ft = (DeleteFreeActionsFactory.SVFluentTemplate) template;
                List<InstanceRef> args = new ArrayList<>();
                for (LVarRef lArg : ft.sv.jArgs())
                    args.add(container.valueOf(lArg, pb));
                GStateVariable sv = store.getGStateVariable(ft.sv.func(), args);
                Fluent f = store.getFluent(sv, container.valueOf(ft.value, pb));
                return (SVFluent) store.get(SVFluent.class, Collections.singletonList(f));
            } else if(template instanceof DeleteFreeActionsFactory.SVFluentWithChangeTemplate) {
                DeleteFreeActionsFactory.SVFluentWithChangeTemplate ft = (DeleteFreeActionsFactory.SVFluentWithChangeTemplate) template;
                List<InstanceRef> args = new ArrayList<>();
                for(LVarRef lArg : ft.sv.jArgs())
                    args.add(container.valueOf(lArg, pb));
                GStateVariable sv = store.getGStateVariable(ft.sv.func(), args);
                Fluent f = store.getFluent(sv, container.valueOf(ft.value, pb));
                return (SVFluentWithChange) store.get(SVFluentWithChange.class, Collections.singletonList(f));
            } else if(template instanceof DeleteFreeActionsFactory.TaskFluentTemplate){
                DeleteFreeActionsFactory.TaskFluentTemplate tft = (DeleteFreeActionsFactory.TaskFluentTemplate) template;
                List<InstanceRef> args = new ArrayList<>();
                for(LVarRef lArg : tft.args)
                    args.add(container.valueOf(lArg, pb));
                return (TaskPropFluent) store.get(TaskPropFluent.class, Arrays.asList(tft.prop, store.getTask(tft.taskName, args)));
            } else if(template instanceof DeleteFreeActionsFactory.ActionPossibleTemplate) {
                return (ActionPossible) store.get(ActionPossible.class, Collections.singletonList(container));
            } else {
                throw new FAPEException("Unsupported template: "+template);
            }
        }

        public static DGFluent getBasicFluent(Fluent f, GStore store) {
            return (SVFluent) store.get(SVFluent.class, Collections.singletonList(f));
        }
        public static DGFluent getFluentWithChange(Fluent f, GStore store) {
            return (SVFluentWithChange) store.get(SVFluentWithChange.class, Collections.singletonList(f));
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

        @Override
        public String toString() { return "Subaction-startable: "+act.toString()+"__"+tp; }
    }
    /** A fluent that is part of a causal link (can only support persistences) */
    @Ident(DependencyGraph.Node.class) public static class SVFluent extends DGFluent {
        public final Fluent fluent;
        @ValueConstructor @Deprecated
        public SVFluent(Fluent f) { this.fluent = f; }

        @Override
        public String toString() { return fluent.toString(); }
    }
    /** a fluent that is not part of any causal link (can support either persistences or transitions */
    @Ident(DependencyGraph.Node.class) public static class SVFluentWithChange extends DGFluent {
        public final Fluent fluent;
        @ValueConstructor @Deprecated
        public SVFluentWithChange(Fluent f) { this.fluent = f; }

        @Override
        public String toString() { return fluent.toString(); }
    }
    @Ident(DependencyGraph.Node.class) public static class TaskPropFluent extends DGFluent {
        public final String proposition;
        public final GTask task;
        @ValueConstructor @Deprecated
        public TaskPropFluent(String prop, GTask task) { this.proposition = prop; this.task = task; }
        public String toString() { return proposition+"("+task+")"; }
    }

    @Ident(DependencyGraph.Node.class)
    public static class ActionPossible extends DGFluent {
        public final GAction action;
        @ValueConstructor @Deprecated
        public ActionPossible(GAction action) { this.action = action; }
    }

    public final int time;
    public final DGFluent fluent;

    @Override public String toString() { return time+": "+fluent; }

    public static TempFluent from(DeleteFreeActionsFactory.TempFluentTemplate template, GAction container, GroundProblem pb, GStore store) {
        int time = container.evaluate(template.time);
        DGFluent fluent = DGFluent.from(template.fluent, container, pb.liftedPb, store);
        return new TempFluent(time, fluent);
    }
}
