package fape.core.inference;

import java.util.ArrayList;
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
    private ArrayList<Integer>[] varsAppearance;

    // current number of vars
    private int numVars = 0;

    // current number of clauses
    private int numClauses = 0;

    public Reasoner(int maxVars, int maxClauses) {
        clauseLeftVar = new int[maxClauses];
        clausePending = new int[maxClauses];
        varsStatus = new boolean[maxVars];
        varsAppearance = new ArrayList[maxVars];
    }

    void addVar(int var) {
        if(var >= varsStatus.length) {
            varsStatus = Arrays.copyOf(varsStatus, varsStatus.length*2);
            varsAppearance = Arrays.copyOf(varsAppearance, varsAppearance.length*2);
        }
        assert var < varsStatus.length;
        while(numVars <= var) {
            varsStatus[numVars] = false;
            varsAppearance[numVars] = new ArrayList<>(numAppearence);
            numVars++;
        }
    }

    void addHornClause(int left, int... right) {
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
                varsAppearance[v].add(clauseNum);
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
