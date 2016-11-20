package fr.laas.fape.acting;

import fr.laas.fape.anml.model.concrete.Action;
import fr.laas.fape.anml.model.concrete.GlobalRef;
import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.constraints.stnu.dispatching.DispatchableNetwork;
import fr.laas.fape.planning.core.planning.states.Printer;
import fr.laas.fape.planning.core.planning.states.State;
import planstack.structures.IList;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlanDispatcher {

    public static void simulateExecution(State plan) {
        Map<TPRef, Action> actionStarts = plan.getAllActions().stream()
                .filter(a -> a.start().genre().isDispatchable())
                .collect(Collectors.toMap(Action::start, Function.identity()));
        Map<TPRef, Action> actionEnds = plan.getAllActions().stream()
                .filter(a -> a.end().genre().isDispatchable())
                .collect(Collectors.toMap(Action::end, Function.identity()));

        Set<TPRef> observable = plan.csp.stn().timepoints().stream().collect(Collectors.toSet());
        DispatchableNetwork<GlobalRef> dispatchableNetwork = DispatchableNetwork.getDispatchableNetwork(plan.csp.stn(), observable);

        int currentTime = 0;
        dispatchableNetwork.setExecuted(plan.csp.stn().getStartTimePoint().get(), currentTime);
        while (!dispatchableNetwork.isExecuted(plan.csp.stn().getEndTimePoint().get())) {
            IList<TPRef> executables = dispatchableNetwork.getExecutables(currentTime);
            for(TPRef tp : executables) {
                if(actionStarts.containsKey(tp)) {
                    System.out.printf("[%d] Starting: %s\n", currentTime, Printer.action(plan, actionStarts.get(tp)));
                }
                if(actionEnds.containsKey(tp)) {
                    System.out.printf("[%d] Ending:   %s\n", currentTime, Printer.action(plan, actionEnds.get(tp)));
                }
                dispatchableNetwork.setExecuted(tp, currentTime);
            }
            currentTime++;
        }
        System.out.printf("[%d] Plan fully executed\n", currentTime);
    }
}
