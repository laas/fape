package fr.laas.fape.planning.core.inference;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LeveledReasonerTests {


    private static int[] arr(int... values) {
        return values.clone();
    }

    public static LeveledReasoner getBase() {
        LeveledReasoner r = new LeveledReasoner();
        r.addClause(arr(1, 2), arr(3, 6), 0);
        r.addClause(arr(4, 5), arr(6), 1);
        r.addClause(arr(2, 3), arr(4), 2);
        return r;
    }

    @Test
    public void testPath() {
        LeveledReasoner r = getBase();
        r.set(1);
        r.set(2);
        r.set(5);
        r.infer();

        assertEquals(r.getPathTo(1).size(), 0);
        assertEquals(r.getPathTo(2).size(), 0);
        assertEquals(r.getPathTo(5).size(), 0);

        assertEquals(r.getPathTo(3).size(), 1);
        assert(r.getPathTo(3).contains(0));

        assertEquals(r.getPathTo(4).size(), 2);
        assert(r.getPathTo(4).contains(0) && r.getPathTo(4).contains(2));

        assertEquals(r.getPathTo(6).size(), 1);
        assert(r.getPathTo(6).contains(0));

    }

    @Test
    public void testClauseAndFactsLevels() {
        LeveledReasoner r = getBase();
        r.set(1);
        r.set(2);
        r.set(5);

        r.infer();
        assertEquals(0, r.levelOfFact(1));
        assertEquals(0, r.levelOfFact(2));
        assertEquals(0, r.levelOfFact(5));
        assertEquals(1, r.levelOfFact(3));
        assertEquals(1, r.levelOfFact(6));
        assertEquals(2, r.levelOfFact(4));
        assertEquals(-1, r.levelOfFact(0));

        assertEquals(1, r.levelOfClause(0));
        assertEquals(3, r.levelOfClause(1));
        assertEquals(2, r.levelOfClause(2));
    }

    @Test
    public void testClauseAndFactsLevelsOnClone() {
        LeveledReasoner r = getBase();
        r.set(1);
        r.set(2);
        r.set(5);

        r.infer();

        LeveledReasoner r2 = new LeveledReasoner(r, null);
        r2.set(3); r2.set(2); r2.set(5);
        r2.infer();
        assertEquals(0, r2.levelOfFact(2));
        assertEquals(0, r2.levelOfFact(3));
        assertEquals(0, r2.levelOfFact(5));
        assertEquals(1, r2.levelOfFact(4));
        assertEquals(2, r2.levelOfFact(6));
        assertEquals(-1, r2.levelOfFact(0));
        assertEquals(-1, r2.levelOfFact(1));

        assertEquals(1, r2.levelOfClause(2));
        assertEquals(2, r2.levelOfClause(1));
        assertEquals(-1, r2.levelOfClause(0));

        assertEquals(0, r.levelOfFact(1));
        assertEquals(0, r.levelOfFact(2));
        assertEquals(0, r.levelOfFact(5));
        assertEquals(1, r.levelOfFact(3));
        assertEquals(1, r.levelOfFact(6));
        assertEquals(2, r.levelOfFact(4));
        assertEquals(-1, r.levelOfFact(0));

        assertEquals(1, r.levelOfClause(0));
        assertEquals(3, r.levelOfClause(1));
        assertEquals(2, r.levelOfClause(2));
    }
}
