package fr.laas.fape.planning.core.inference;


import fr.laas.fape.planning.core.planning.heuristics.DefaultIntRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class HLeveledReasonerTests {

    private static int[] arr(int... values) {
        return values.clone();
    }
    private static String[] arr(String... values) {
        return values.clone();
    }

    @Test
    public void testBase() {
        HLeveledReasoner<String, String> hlr = new HLeveledReasoner<>(new DefaultIntRepresentation<>(), new DefaultIntRepresentation<>());

        hlr.addClause(arr("1", "2"), arr("3"), "A");
        hlr.addClause(arr("3"), arr("4"), "B");
        hlr.set("1");
        hlr.set("2");
        hlr.infer();


        assertEquals(hlr.getSteps("4").size(), 2);
        assert(hlr.getSteps("4").contains("A") && hlr.getSteps("4").contains("B"));

    }
}
