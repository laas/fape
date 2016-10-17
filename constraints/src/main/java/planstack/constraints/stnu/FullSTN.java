package planstack.constraints.stnu;

import planstack.constraints.stn.CoreSTN;
import planstack.structures.IList;
import planstack.structures.ISet;
import scala.*;

import java.util.LinkedList;

/**
 * Simple Temporal Network, provides effective representation and copy constructor, methods for acessing,
 * adding and checking constraints and time points, and implementation of IFPC algorithm.
 *
 * Notice this class is the bottle-neck of the planning system. Some operations could and should be implemented
 * more efficiently, e.g. accessing constraints contains "if", this could be avoided by smart indexing and preprocessing
 * of indexing operations.
 *
 * IFPC is a subject to further research, notably delayed propagation should be tried in context of ordering queries
 * on STN. The aggregated propagation could especially helpful when there is multiple new constraints in a sequence.
 * We partially explored extension of IFPC this way, althougt effective implementation requires sophisticated data structures,
 * which we didn't have time to implement.
 *
 * Another extension might contain change from copying the network to maintaing backtrackable network, generally there is not
 * too many points we need to remember for backtracking.
 * @author Filip Dvořák
 */
public class FullSTN<ID> extends CoreSTN<ID> {

    private static int pos(int i, int j) {
        return i*(i-1)/2 + j;
    }

    /** lower bound on time relations */
    private int edge_a[];
    /** upper bound on time relations */
    private int edge_b[];
    /** value of the last increment of the number of edges in the network */
    private int last_inc;
    /** current capacity of the network (in number of time points) */
    private int capacity;
    /** id of the latest time point inserted */
    private int top;

    private IList<Tuple4<Integer,Integer,Integer,ID>> allConstraints;
    private ISet<Integer> emptySpots;

    private boolean consistent;

    /** -infinity */
    private static final int inf = -2000000000;
    /** +infinity */
    private static final int sup = 2000000000;

    /**
     * constructs new network (intended to run once per Finder invokation)
     */
    public FullSTN(){
        init(10);
    }

    public FullSTN(int initial_capacity){
        init(initial_capacity);
    }

    private void init(int capacity) {
        allConstraints = new IList<>();
        emptySpots = new ISet<>();
        top = 0;
        edge_a = new int[capacity * capacity / 2];
        edge_b = new int[capacity * capacity / 2];
        this.capacity = capacity;
        last_inc = capacity * capacity / 2;
        consistent = true;
        add_v(); //start
        add_v(); //end
        enforceBefore(start(), end());
    }

    /**
     * copy-constructor, prealocates space for new edges, if needed
     * @param s
     */
    private FullSTN(FullSTN<ID> s){
        this.allConstraints = s.allConstraints;
        this.emptySpots = s.emptySpots;
        this.capacity = s.capacity;
        this.top = s.top;
        this.last_inc = s.last_inc;
        this.consistent = s.consistent;

        if(capacity - top < 5){ //!!IMPORTANT
            last_inc += 100;
            capacity += 10;
            edge_a = new int[s.edge_a.length + last_inc];
            edge_b = new int[s.edge_b.length + last_inc];
        }
        else
        {
            edge_a = new int[s.edge_a.length];
            edge_b = new int[s.edge_b.length];
        }
        System.arraycopy(s.edge_a, 0, edge_a, 0, top*(top-1)/2);
        System.arraycopy(s.edge_b, 0, edge_b, 0, top*(top-1)/2);
    }

    protected int minDelay(int var1, int var2){
        if(var1 == var2) return 0;
        else if(var1 < var2) return (- maxDelay(var2, var1));
        else return edge_a[pos(var1, var2)];
    }
    /**
     * returns the upper bound on time between time point var1 and var2, uses symmetry of the network
     * @param var1 time point
     * @param var2 time point
     * @return lower bound on time between time point var1 and var2
     */
    public int maxDelay(int var1, int var2){
        if(var1 == var2) return 0;
        else if(var1 < var2) return (- minDelay(var2, var1));
        else return edge_b[pos(var1, var2)];
    }
    /**
     * sets value of the lower bound on time between time point var1 and var2, uses symmetry of the network
     *
     * Notice, this method is used directly only in propagation
     *
     * @param var1 time point
     * @param var2 time point
     * @param value time
     */
    private void sa(int var1, int var2, int value){
        if(var1 < var2) sb(var2,var1, - value);
        else edge_a[pos(var1, var2)] = value;
    }
    /**
     * sets value of the upper bound on time between time point var1 and var2, uses symmetry of the network
     *
     * Notice, this method is used directly only in propagation
     *
     * @param var1 time point
     * @param var2 time point
     * @param value time
     */
    private void sb(int var1, int var2, int value){
        if(var1 < var2) sa(var2,var1, - value);
        else edge_b[pos(var1, var2)] = value;
    }

    /**
     * adds new time point into the network. In fact adds only edges and sets them to (inf,sup)
     * @return id of the newly inserted time point
     */
    private int add_v(){
        if(top >= capacity)
        {
            last_inc += 100;
            capacity += 10;
            int pma[] = new int[edge_a.length + last_inc], pmb[] = new int[edge_b.length + last_inc];
            System.arraycopy(edge_a, 0, pma, 0, top*(top-1)/2);
            System.arraycopy(edge_b, 0, pmb, 0, top*(top-1)/2);
            edge_a = pma;
            edge_b = pmb;
        }
        int st = top*(top-1)/2, ct = (top+1)*(top)/2;
        for(int i=st; i < ct;i++){
            edge_a[i] = inf;
            edge_b[i] = sup;
        }
        top++;
        return top-1;
    }
    /**
     * Determines, if the edge (v1,v2) can be consistently updated by constraint (a,b).
     * @param v1 time point
     * @param v2 time point
     * @param a lower bound on time
     * @param b upper bound on tim
     * @return true if consistent
     */
    private boolean edge_consistent(int v1, int v2, int a, int b){
        //non-empty intersection
        return (Math.max(minDelay(v1, v2),a) <= Math.min(maxDelay(v1, v2), b));
    }
    /**
     * addition operation that takes into account our infities
     * @param a time
     * @param b time
     * @return product of addition operation on a and b
     */
    private static int lim_plus(int a, int b){
        return (a == inf || b == inf) ? inf : (a == sup || b == sup)?sup:a+b;
    }

    /**
     * Implementation of IFPC. The bottle-neck.
     *
     * Assumes the new constraint is consistent.
     *
     * @param v1 time point
     * @param v2 time point
     * @param a lower bound on time
     * @param b upper bound on tim
     */
    private void propagate(int v1, int v2, int a, int b){
        if(!consistent || !edge_consistent(v1, v2, a, b)) {
            consistent = false;
            throw new InconsistentTemporalNetwork();
        }

        int I_top = 0, J_top = 0, I[] = new int[top], J[] = new int[top], aa, bb, i,j,k;

        aa = Math.max(a, minDelay(v1, v2)); //(K->V1 + V1->V2) /\ (K->V2)
        bb = Math.min(b, maxDelay(v1, v2));
        if(minDelay(v1, v2) != aa || maxDelay(v1, v2) != bb){

            // intersection of the original edge
            sa(v1, v2, aa);
            sb(v1, v2, bb);

            // propagation to all connected edges
            for(k = 0; k < top; k++) {
                if(emptySpots.contains(k)) break;
                if (v1 != k && v2 != k) {
                    aa = lim_plus(minDelay(k, v1), minDelay(v1, v2)); //K->V1 + V1->V2
                    bb = lim_plus(maxDelay(k, v1), maxDelay(v1, v2));
                    aa = Math.max(aa, minDelay(k, v2)); //(K->V1 + V1->V2) /\ (K->V2)
                    bb = Math.min(bb, maxDelay(k, v2));
                    if (minDelay(k, v2) != aa || maxDelay(k, v2) != bb) {//change of interval occured
                        I[I_top++] = k;
                        sa(k, v2, aa);
                        sb(k, v2, bb);
                    }
                    aa = lim_plus(minDelay(v1, v2), minDelay(v2, k)); //V1->V2 + V2->K
                    bb = lim_plus(maxDelay(v1, v2), maxDelay(v2, k));
                    aa = Math.max(aa, minDelay(v1, k)); //(V1->V2 + V2->K) /\ (V1->K)
                    bb = Math.min(bb, maxDelay(v1, k));
                    if (minDelay(v1, k) != aa || maxDelay(v1, k) != bb) {//change of interval occured
                        J[J_top++] = k;
                        sa(v1, k, aa);
                        sb(v1, k, bb);
                    }
                }
            }

            for(i = 0; i < I_top; i++) {
                if(emptySpots.contains(i)) break;
                for (j = 0; j < J_top; j++) {
                    if(emptySpots.contains(j)) break;
                    if (I[i] != J[j]) {
                        aa = lim_plus(minDelay(I[i], v1), minDelay(v1, J[j])); //I->V1 + V1->J
                        bb = lim_plus(maxDelay(I[i], v1), maxDelay(v1, J[j]));
                        aa = Math.max(aa, minDelay(I[i], J[j])); //(I->V1 + V1->J) /\ (I->J)
                        bb = Math.min(bb, maxDelay(I[i], J[j]));
                        if (minDelay(I[i], J[j]) != aa || maxDelay(I[i], J[j]) != bb) {
                            sa(I[i], J[j], aa);
                            sb(I[i], J[j], bb);
                        }
                    }
                }
            }
        }
    }

    public boolean isConsistent() {
        return consistent;
    }

    @Override
    public boolean consistent() {
        return isConsistent();
    }

    @Override
    public int addVar() {
        int u;
        if(emptySpots.isEmpty())
            u = this.add_v();
        else {
            u = emptySpots.head();
            emptySpots = emptySpots.without(u);
        }
        enforceBefore(u, end());
        return u;
    }

    @Override
    public IList<Object> events() {
        LinkedList<Object> events = new LinkedList<>();
        for(int i=0 ; i<size() ; i++) {
            if(!emptySpots.contains(i))
                events.add(i);
        }
        return new IList<>(events);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IList<Tuple5<Object, Object, Object, ElemStatus, Option<ID>>> constraints() {
        IList<Tuple5<Object, Object, Object, ElemStatus, Option<ID>>> list = new IList<>();
        for(Tuple4<Integer,Integer,Integer,ID> cons : allConstraints) {
            Tuple5<Object, Object, Object, ElemStatus, Option<ID>> current;
            if(cons._4() != null)
                current = new Tuple5<>(cons._1(), cons._2(), cons._3(), ElemStatus.CONTROLLABLE, new Some<>(cons._4()));
            else
                current = new Tuple5<>(cons._1(), cons._2(), cons._3(), ElemStatus.CONTROLLABLE, None$.empty());
            list = list.with(current);
        }
        return list;
    }

    @Override
    public int size() {
        return this.top - emptySpots.size();
    }

    @Override
    public boolean addConstraint(int u, int v, int w) {
        allConstraints = allConstraints.with(new Tuple4<>(u, v, w, null));
        propagate(u, v, inf, w);
        return consistent();
    }

    @Override
    public boolean addConstraintWithID(int u, int v, int w, ID o) {
        allConstraints = allConstraints.with(new Tuple4<>(u, v, w, o));
        propagate(u, v, inf, w);
        return consistent();
    }

    @Override
    public boolean checkConsistency() {
        return consistent();
    }

    @Override
    public boolean checkConsistencyFromScratch() {
        int size = this.top;
        ISet<Integer> emptySpotsCC = emptySpots;
        IList<Tuple4<Integer,Integer,Integer,ID>> constraints = this.allConstraints;
        init(size);
        // add all timepoints except start and end that are defined in init
        for(int i=2 ; i<size ; i++)
            addVar();//TODO
        // restore empty spots
        emptySpots = emptySpotsCC;
        for(Tuple4<Integer,Integer,Integer,ID> c : constraints) {
            if(c._4() != null)
                addConstraintWithID(c._1(), c._2(), c._3(), c._4());
            else
                addConstraint(c._1(), c._2(), c._3());
        }
        return checkConsistency();
    }

    @Override
    public void writeToDotFile(String file) {
        // TODO
    }

    @Override
    public int earliestStart(int u) {
        return minDelay(start(), u);
    }

    @Override
    public int latestStart(int u) {
        return maxDelay(start(), u);
    }

    @Override
    public boolean isConstraintPossible(int u, int v, int w) {
        return edge_consistent(u, v, inf, w);
    }

    @Override
    public boolean removeConstraintsWithID(ID id) {
        IList<Tuple4<Integer, Integer, Integer, ID>> filtered = new IList<>();
        for(Tuple4<Integer, Integer, Integer, ID> c : allConstraints) {
            if(c._4() == null || !c._4().equals(id))
                filtered = filtered.with(c);
        }
        allConstraints = filtered;
        return checkConsistencyFromScratch();
    }

    @Override
    public boolean removeVar(int u) {
        IList<Tuple4<Integer, Integer, Integer, ID>> filtered = new IList<>();
        for(Tuple4<Integer, Integer, Integer, ID> c : allConstraints) {
            if(c._1() != u && c._2() != u)
                filtered = filtered.with(c);
        }
        emptySpots = emptySpots.with(u);
        allConstraints = filtered;
        return checkConsistencyFromScratch();
    }

    @Override
    public FullSTN<ID> cc() {
        return new FullSTN<>(this);
    }
}
