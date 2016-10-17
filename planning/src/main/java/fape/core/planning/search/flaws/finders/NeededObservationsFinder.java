package fape.core.planning.search.flaws.finders;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fape.core.planning.states.State;
import fape.core.planning.states.StateExtension;
import fape.exceptions.FAPEException;
import fape.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Value;
import planstack.anml.model.concrete.*;
import planstack.constraints.stnu.morris.PartialObservability;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This flaw finder and the associated resolvers are experimental and will right now only work in the rabbits domain.
 */
public class NeededObservationsFinder implements FlawFinder {


    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        Stream<TPRef> contingents = st.csp.stn().timepoints().stream().filter(TPRef::isContingent);

        if(!st.hasExtension(PartialObservabilityExt.class))
            st.addExtension(new PartialObservabilityExt(new HashSet<>(), new HashMap<>()));

        PartialObservabilityExt obs = st.getExtension(PartialObservabilityExt.class);
        Set<TPRef> observable = contingents
                .filter(tp -> !obs.observed.contains(tp) && obs.observationConditions.containsKey(tp))
                .collect(Collectors.toSet());

        Optional<PartialObservability.NeededObservations> opt = PartialObservability.getResolvers(st.csp.stn().constraints().stream().collect
                (Collectors.toList()), obs.observed, observable);

        if(opt.isPresent())
            return Collections.singletonList(new NeededObservationFlaw(opt.get(), st));
        else
            return Collections.emptyList();
    }


    @Value private static final class PartialObservabilityExt implements StateExtension {
        public final Set<TPRef> observed;
        public final Map<TPRef,Chronicle> observationConditions;

        @Override
        public StateExtension clone(State st) {
            return new PartialObservabilityExt(new HashSet<>(observed), new HashMap<>(observationConditions));
        }

        @Override
        public void chronicleMerged(Chronicle c) {
            for(ChronicleAnnotation annot : c.annotations()) {
                if(annot instanceof ObservationConditionsAnnotation) {
                    ObservationConditionsAnnotation obsCond = (ObservationConditionsAnnotation) annot;
                    observationConditions.put(obsCond.tp(), obsCond.conditions());
                    if(obsCond.conditions().isEmpty())
                        observed.add(obsCond.tp());
                }
            }
        }
    }

    public static final class NeededObservationFlaw extends Flaw {
        final Collection<Set<TPRef>> possibleObservationsSets;

        public NeededObservationFlaw(PartialObservability.NeededObservations no, State st) {
            possibleObservationsSets = no.resolvingObs();
        }

        @Override
        public List<Resolver> getResolvers(State st, Planner planner) {
            return possibleObservationsSets.stream().map(NeededObsResolver::new).collect(Collectors.toList());
        }

        @Override
        public int compareTo(Flaw o) {
            throw new FAPEException("There should not be two needed observations flaws on the same state.");
        }

        @AllArgsConstructor final class NeededObsResolver implements Resolver {
            public final Set<TPRef> toObserve;

            @Override
            public boolean apply(State st, Planner planner, boolean isFastForwarding) {
                PartialObservabilityExt obs = st.getExtension(PartialObservabilityExt.class);
                toObserve.stream().forEach(tp -> {
                    st.applyChronicle(obs.getObservationConditions().get(tp));
                    obs.observed.add(tp);
                });

                return true;
            }

            @Override
            public int compareWithSameClass(Resolver e) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }
    }
}
