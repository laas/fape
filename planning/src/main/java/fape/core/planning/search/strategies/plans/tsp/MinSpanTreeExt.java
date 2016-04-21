package fape.core.planning.search.strategies.plans.tsp;

import fape.core.planning.grounding.Fluent;
import fape.core.planning.grounding.GAction;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.core.planning.timelines.Timeline;
import planstack.anml.model.concrete.statements.LogStatement;
import fape.core.planning.grounding.GAction.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MinSpanTreeExt implements StateExtension {

    final State st;

    final HashMap<LogStatement, HashSet<GLogStatement>> groundStatements = new HashMap<>();

    public MinSpanTreeExt(State st) {
        this.st = st;

        process();
    }

    private void process() {
        for(Timeline tl : st.getTimelines()) {
            if(tl.hasSinglePersistence())
                continue;

            Set<LogStatement> restricted = new HashSet<>();

            for(int i=1; i<tl.numChanges(); i++) {
                // fluents supported by statement i-1
                Set<Fluent> supportfluents = getGrounded(tl.getEvent(i-1)).stream()
                        .map(gs -> endFluent(gs))
                        .collect(Collectors.toSet());

                // restrict instantiations of statement i to those supported by i-1
                boolean removal = getGrounded(tl.getEvent(i))
                        .removeIf(gs -> !supportfluents.contains(startFluent(gs)));
                if(removal)
                    restricted.add(tl.getEvent(i));
            }

            for(int i=tl.numChanges()-2; i>0; i++) {
                // fluents supported by statement i-1
                Set<Fluent> supportfluents = getGrounded(tl.getEvent(i+1)).stream()
                        .map(gs -> startFluent(gs))
                        .collect(Collectors.toSet());

                // restrict instantiations of statement i to those supported by i-1
                boolean removal = getGrounded(tl.getEvent(i))
                        .removeIf(gs -> !supportfluents.contains(endFluent(gs)));
                if(removal)
                    restricted.add(tl.getEvent(i));
            }

            if(!restricted.isEmpty())
                System.out.println(restricted);
        }
    }

    private HashSet<GAction.GLogStatement> getGrounded(LogStatement s) {
        if(!groundStatements.containsKey(s)) {
            groundStatements.put(s, new HashSet<>(st.getGroundStatements(s)));
        }
        return groundStatements.get(s);
    }
    private Fluent startFluent(GAction.GLogStatement s) {
        return st.pl.preprocessor.getFluent(s.sv, s.startValue());
    }
    private Fluent endFluent(GAction.GLogStatement s) {
        return st.pl.preprocessor.getFluent(s.sv, s.endValue());
    }

    @Override
    public StateExtension clone(State st) {
        return new MinSpanTreeExt(st);
    }
}
