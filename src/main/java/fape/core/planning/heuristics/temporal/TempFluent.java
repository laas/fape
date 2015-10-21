package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GTaskCond;
import fape.core.planning.grounding.GroundProblem;
import fape.exceptions.FAPEException;
import lombok.Value;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.concrete.InstanceRef;
import scala.Array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Value public class TempFluent {

    @Value public static class Fluent implements DepGraph.Node {
        public final String funcName;
        public final List<InstanceRef> argsAndValue;

        private Fluent(String funcName, InstanceRef[] argsAndValue) {
            this.funcName = funcName;
            this.argsAndValue = Arrays.asList(argsAndValue);
        }

        public boolean isTask() { return funcName.startsWith("task-"); }
        public boolean isActionStart() { return funcName.startsWith("started-"); }

        public static Fluent from(DeleteFreeActionsFactory.TempFluentTemplate template, GAction container, AnmlProblem pb) {
            InstanceRef[] argsAndValue = new InstanceRef[template.args.length+1];
            for(int i=0 ; i<template.args.length ; i++) {
                argsAndValue[i] = container.valueOf(template.args[i], pb);
            }
            argsAndValue[argsAndValue.length-1] = container.valueOf(template.value, pb);
            return new Fluent(template.funcName, argsAndValue);
        }

        public static Fluent from(fape.core.planning.grounding.Fluent f) {
            InstanceRef[] args = Arrays.copyOf(f.sv.params, f.sv.params.length+1);
            args[args.length-1] = f.value;
            return new Fluent(f.sv.f.name(), args);
        }
        public static Fluent from(GTaskCond task, AnmlProblem pb) {
            InstanceRef[] args = Arrays.copyOf(task.args, task.args.length+1);
            args[args.length-1] = pb.instance("true"); //TODO: fix this hack
            return new Fluent("task-"+task.name, args);
        }
    }

    public final int time;
    public final Fluent fluent;


    public static TempFluent from(DeleteFreeActionsFactory.TempFluentTemplate template, GAction container, GroundProblem pb) {
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

        Fluent fluent = Fluent.from(template, container, pb.liftedPb);

        return new TempFluent(time, fluent);
    }
}
