package fr.laas.fape.planning.core.inference;

import java.util.Arrays;

public class Reasoner {

    private static final int numAppearence = 3;

    // variable at the left side of the clause
    private int[] clauseLeftVar;

    // number of terms that are not proven yet
    // if it gets to 0, it means we can infer its left side
    private int[] clausePending;

    // current status of a var, set to false until it is proven to be true
    protected boolean[] varsStatus;

    // clauses in which this variable appears
    private int[][] varsAppearance;

    // current number of vars
    private int numVars = 0;

    // current number of clauses
    private int numClauses = 0;

    // if true, it is forbidden to add clauses or variables to this Reasoner.
    // this is used to share clauses between clones of a same reasoner.
    private boolean locked = false;

    public Reasoner(int maxVars, int maxClauses) {
        clauseLeftVar = new int[maxClauses];
        clausePending = new int[maxClauses];
        varsStatus = new boolean[maxVars];
        varsAppearance = new int[maxVars][];
    }

    public Reasoner(Reasoner toCopy, boolean lock) {
        this.locked = lock;
        this.numClauses = toCopy.numClauses;
        this.numVars = toCopy.numVars;
        clausePending = Arrays.copyOfRange(toCopy.clausePending, 0, numClauses);
        varsStatus = Arrays.copyOfRange(toCopy.varsStatus, 0, numVars);

        if(locked && toCopy.locked) {
            clauseLeftVar = toCopy.clauseLeftVar;
            varsAppearance = toCopy.varsAppearance;
        } else {
            clauseLeftVar = Arrays.copyOfRange(toCopy.clauseLeftVar, 0, numClauses);
            varsAppearance = new int[numVars][];
            for (int i = 0; i < numVars; i++)
                varsAppearance[i] = Arrays.copyOf(toCopy.varsAppearance[i], toCopy.varsAppearance.length);
        }
    }

    public void lock() {
        this.locked = true;
    }

    void addVar(int var) {
        assert !locked;
        if(var >= varsStatus.length) {
            varsStatus = Arrays.copyOf(varsStatus, varsStatus.length*2);
            varsAppearance = Arrays.copyOf(varsAppearance, varsAppearance.length*2);
        }
        assert var < varsStatus.length;
        while(numVars <= var) {
            varsStatus[numVars] = false;
            varsAppearance[numVars] = new int[0];
            numVars++;
        }
    }

    void addHornClause(int left, int... right) {
        assert !locked;
        if(left >= numVars)
            addVar(left);

        if(right.length == 0) {
            set(left);
        } else {
            int clauseNum = numClauses++;
            if(numClauses >= clauseLeftVar.length) {
                clauseLeftVar = Arrays.copyOf(clauseLeftVar, clauseLeftVar.length*2);
                clausePending = Arrays.copyOf(clausePending, clausePending.length*2);
            }
                for (int v : right)
                if (v >= numVars)
                    addVar(v);

            clauseLeftVar[clauseNum] = left;
            clausePending[clauseNum] = right.length;
            for (int v : right) {
                // add one clause to the vars appearance of v
                varsAppearance[v] = Arrays.copyOf(varsAppearance[v], varsAppearance[v].length+1);
                varsAppearance[v][varsAppearance[v].length-1] = clauseNum;
                if(varsStatus[v])
                    clausePending[clauseNum]--;
            }
            if(clausePending[clauseNum] == 0)
                set(clauseLeftVar[clauseNum]);
        }
    }

    public void set(int var) {
        assert var < numVars;

        if(!varsStatus[var]) {
            varsStatus[var] = true;
            for(int clause : varsAppearance[var]) {
                clausePending[clause]--;
                if(clausePending[clause] == 0)
                    set(clauseLeftVar[clause]);
            }
        }
    }

    public void printVars() {
        for(int i=0 ; i<numVars ; i++) {
            if(varsStatus[i]) System.out.print(i+" ");
            else System.out.print("-"+i+" ");
        }
        System.out.println();
    }

    public static void main(String[] args) {

        Reasoner r = new Reasoner(10,10);
        int[] right = {1,2,3};
        int left = 4;

        r.addHornClause(left, right);
        r.printVars();
        r.set(1);
        r.set(1);r.set(1);
        r.printVars();
        r.set(3);
        r.printVars();
        r.set(2);
        r.printVars();

        r.addHornClause(5);
        r.addHornClause(6,3,7);
        r.addHornClause(6,1);
        r.printVars();
    }

}
