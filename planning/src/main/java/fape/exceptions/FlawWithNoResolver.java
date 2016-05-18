package fape.exceptions;

import fape.core.planning.search.flaws.flaws.Flaw;
import fr.laas.fape.exceptions.InconsistencyException;
import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor @ToString
public class FlawWithNoResolver extends InconsistencyException {
    public final Flaw flaw;
}
