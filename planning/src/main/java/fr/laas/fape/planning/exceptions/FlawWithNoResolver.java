package fr.laas.fape.planning.exceptions;

import fr.laas.fape.exceptions.InconsistencyException;
import fr.laas.fape.planning.core.planning.search.flaws.flaws.Flaw;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor @ToString
public class FlawWithNoResolver extends InconsistencyException {
    public final Flaw flaw;
}
