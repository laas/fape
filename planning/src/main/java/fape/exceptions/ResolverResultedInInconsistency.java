package fape.exceptions;

import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.resolvers.Resolver;
import fr.laas.fape.exceptions.InconsistencyException;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor @ToString
public class ResolverResultedInInconsistency extends InconsistencyException {
    final Flaw flaw;
    final Resolver resolver;
}
