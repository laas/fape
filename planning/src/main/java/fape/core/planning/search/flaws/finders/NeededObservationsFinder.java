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
import planstack.anml.model.Function;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.*;
import planstack.anml.model.concrete.statements.LogStatement;
import planstack.anml.model.concrete.statements.Persistence;
import planstack.constraints.experimental.PartialObservability;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This flaw finder and the associated resolvers are experimental and will right now only work in the rabbits domain.
 */
public class NeededObservationsFinder implements FlawFinder {


    @Override
    public List<Flaw> getFlaws(State st, Planner planner) {
        Stream<TPRef> actionEnds = st.getAllActions().stream().map(Action::end);
        Stream<TPRef> contingents = st.csp.stn().timepoints().stream().filter(TPRef::isContingent);

        if(!st.hasExtension(SecuredObservations.class))
            st.addExtension(new SecuredObservations(new HashSet<TPRef>()));

        SecuredObservations obs = st.getExtension(SecuredObservations.class);
        Set<TPRef> observed = Stream.concat(actionEnds, obs.observed.stream()).collect(Collectors.toSet());
        Set<TPRef> observable = contingents.filter(tp -> !observed.contains(tp)).collect(Collectors.toSet());

//        System.out.println("Observed: "+observed);
//        System.out.println("Observable: "+observable);
        Optional<PartialObservability.NeededObservations> opt = PartialObservability.getResolvers(st.csp.stn().constraints().stream().collect
                (Collectors.toList()), observed, observable);

//        System.out.println("Option: "+opt);
        if(opt.isPresent())
            return Collections.singletonList(new NeededObservationFlaw(opt.get(), st));
        else
            return Collections.emptyList();
    }


    @Value public static final class SecuredObservations implements StateExtension {
        public final Set<TPRef> observed;

        @Override
        public StateExtension clone(State st) { return new SecuredObservations(new HashSet<>(observed)); }
    }



    public static final class NeededObservationFlaw extends Flaw {
        final Collection<Set<TPRef>> possibleObservationsSets;
        final Map<TPRef,VarRef> obsLoc = new HashMap<>();
        final Function obsFunc;

        public NeededObservationFlaw(PartialObservability.NeededObservations no, State st) {
            possibleObservationsSets = no.resolvingObs();
            List<TPRef> obsCandidates = st.tdb.allStatements()
                    .filter(s -> s.sv().func().name().equals("Rabbit.at"))
                    .filter(s -> !s.needsSupport())
                    .map(LogStatement::end)
                    .filter(TPRef::isVirtual)
                    .collect(Collectors.toList());
            for(TPRef ctg : possibleObservationsSets.stream().flatMap(Set::stream).collect(Collectors.toSet())) {
                TPRef endOfObservableEvent = obsCandidates.stream()
                        .filter(tp -> tp.isVirtual() && tp.isAttached())
                        .filter(tp -> tp.attachmentToReal()._1().equals(ctg) && tp.attachmentToReal()._2().equals(0))
//                        .filter(tp -> st.csp.stn().constraints().stream().anyMatch(c ->
//                                c.u().equals(ctg) && c.v().equals(tp) && c.d() == 0))
//                        .filter(tp -> st.csp.stn().constraints().stream().anyMatch(c ->
//                                c.v().equals(ctg) && c.u().equals(tp) && c.d() == 0))
                        .findFirst().orElseGet(() -> { throw new FAPEException("No observable event associated"); });
                LogStatement event = st.tdb.allStatements().filter(s -> s.end() == endOfObservableEvent).findAny().get();
                obsLoc.put(ctg, event.endValue());
            }
            obsFunc = st.pb.functions().get("Agent.at");
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
                Chronicle ch = new Chronicle(st.pb);
                List<Pair<TPRef,TPRef>> precedences = new ArrayList<>();
                SecuredObservations obs = st.getExtension(SecuredObservations.class);
                toObserve.stream().forEach(tp -> {
                    assert obsLoc.containsKey(tp) : "Timepoint cannot be observed?";
                    VarRef observerVar = new VarRef(st.pb.instances().asType("Agent"), st.refCounter, new Label("needed-obs",""));
                    st.csp.bindings().addVariable(observerVar);
                    ParameterizedStateVariable sv = new ParameterizedStateVariable(obsFunc, new VarRef[]{observerVar});
                    Persistence p = new Persistence(sv, obsLoc.get(tp), st.pb.chronicles().element(), st.refCounter);
                    ch.statements().add(p);
                    precedences.add(new Pair<>(p.start(), tp));
                    precedences.add(new Pair<>(tp, p.end()));
                    obs.observed.add(tp);
                });

//                ch.initTemporalObjects(); FIXME with new version
                st.applyChronicle(ch);
                for(Pair<TPRef,TPRef> prec : precedences) {
                    st.enforceBefore(prec.value1, prec.value2);
                    st.checkConsistency();
//                    System.out.println(prec.value1 + " : " + st.getEarliestStartTime(prec.value1) + "  " + st.getLatestStartTime(prec.value1));
//                    System.out.println(prec.value2+" : "+st.getEarliestStartTime(prec.value2)+"  "+st.getLatestStartTime(prec.value2));
                }

//                System.out.println("ret == " + new NeededObservationsFinder().getFlaws(st, planner));
                return true;
            }

            @Override
            public int compareWithSameClass(Resolver e) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }
    }




}
