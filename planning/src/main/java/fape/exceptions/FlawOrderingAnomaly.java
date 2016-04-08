package fape.exceptions;

import fape.core.planning.search.flaws.flaws.Flaw;
import lombok.Value;

import java.util.List;

@Value public class FlawOrderingAnomaly extends RuntimeException {
    final List<Flaw> flaws;
    final int flawChoice;
    final int resolverChoice;
}
