package fr.laas.fape.planning.exceptions;

import fr.laas.fape.exceptions.InconsistencyException;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.planning.core.planning.search.flaws.resolvers.Resolver;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor @ToString
public class ResolverResultedInInconsistency extends InconsistencyException {
    final Flaw flaw;
    final Resolver resolver;
}
