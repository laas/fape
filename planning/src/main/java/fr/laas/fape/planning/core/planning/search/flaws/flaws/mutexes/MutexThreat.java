package fr.laas.fape.planning.core.planning.search.flaws.flaws.mutexes;

import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.TemporalConstraint;
import fr.laas.fape.planning.core.planning.states.Printer;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.FluentHolding;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Value @EqualsAndHashCode(callSuper = false)
public class MutexThreat extends Flaw {

    final FluentHolding cl1;
    final FluentHolding cl2;

    @Override
    public List<Resolver> getResolvers(State st, Planner planner) {

        if(MutexThreatsFinder.debug)
            System.out.println("  "+Printer.p(st, this));

        List<Resolver> resolvers = new LinkedList<>();

        if(st.canAllBeBefore(cl1.getEnd(), cl2.getStart())) {
            resolvers.add(new TemporalConstraint(
                    cl1.getEnd(),
                    Collections.singletonList(cl2.getStart()),
                    1, Integer.MAX_VALUE
            ));
            if(MutexThreatsFinder.debug)
                System.out.println("    before");
        }

        if(st.canAllBeBefore(cl2.getEnd(), cl1.getStart())) {
            resolvers.add(new TemporalConstraint(
                    cl2.getEnd(),
                    Collections.singletonList(cl1.getStart()),
                    1, Integer.MAX_VALUE
            ));
            if(MutexThreatsFinder.debug)
                System.out.println("    after");
        }


        return resolvers;
    }

    @Override
    public int compareTo(Flaw o) {
        assert o instanceof MutexThreat;
        MutexThreat mt = (MutexThreat) o;
        if(cl1.getStart().id() != mt.cl1.getStart().id())
            return cl1.getStart().id() - mt.cl1.getStart().id();
        else
            return cl2.getStart().id() - mt.cl2.getStart().id();
    }
}
