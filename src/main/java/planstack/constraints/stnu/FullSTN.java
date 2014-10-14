package planstack.constraints.stnu;

import planstack.constraints.stn.ISTN;
import planstack.structures.IList;
import planstack.structures.ISet;
import scala.*;
import scala.collection.*;

import java.util.LinkedList;
import java.util.List;

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
public class FullSTN<ID> extends ISTN<ID> {

    /**
     *
     */
    public static void precalc_inic(){
        precalc = new int[precalc_size][];
        for(int i = 0; i < precalc_size; i++){
            precalc[i] = new int[i];
            for(int j = 0; j < i; j++){
                precalc[i][j] = i*(i-1)/2 + j;
            }
        }
    }

    /** lower bound on time relations */
    protected int edge_a[];
    /** upper bound on time relations */
    protected int edge_b[];
    /** value of the last increment of the number of edges in the network */
    protected int last_inc;
    /** current capacity of the network (in number of time points) */
    protected int capacity;
    /** id of the latest time point inserted */
    protected int top;

    protected IList<Tuple4<Integer,Integer,Integer,ID>> allConstraints;
    protected ISet<Integer> emptySpots;

    private boolean consistent;

    /** -infinity */
    protected static final int inf = -2000000000;
    /** +infinity */
    protected static final int sup = 2000000000;

    static int precalc[][] = null;
    static final int precalc_size = 10000;

    /**
     * constructs new network (intended to run once per Finder invokation)
     */
    protected FullSTN(){
        init(10);
    }

    public FullSTN(int initial_capacity){
        init(initial_capacity);
    }

    protected void init(int capacity) {
        if(precalc == null)
            precalc_inic();
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
    protected FullSTN(FullSTN<ID> s){
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
    /**
     * corresponds to tp1 is necessarily before tp2
     * @param tp1 time point
     * @param tp2 time point
     * @return tp1 is necessarily before tp2
     */
    protected boolean fless(int tp1, int tp2){ // tp1 <= tp2
        return !edge_consistent(tp1,tp2,inf,-1);
    }
    /**
     * corresponds to tp1 is possibly before tp2
     * @param tp1 time point
     * @param tp2 time point
     * @return tp1 is possibly before tp2
     */
    protected boolean pless(int tp1, int tp2){ // tp1 <= tp2
        return edge_consistent(tp1,tp2,0,sup);
    }
    /**
     * corresponds to tp1 is undefined to tp2
     * @param tp1 time point
     * @param tp2 time point
     * @return tp1 is undefined to tp2
     */
    protected boolean pnfless(int tp1, int tp2){ // tp1 <= tp2
        return (edge_consistent(tp1,tp2,inf,0) && edge_consistent(tp1,tp2,0,sup));
    }
    /**
     * enforces tp1 is necessarily before tp2
     * @param tp1 time point
     * @param tp2 time point
     */
    protected void eless(int tp1, int tp2){
        enforceBefore(tp1, tp2);
    }
    /**
     * returns the lower bound on time between time point var1 and var2, uses symmetry of the network
     * @param var1 time point
     * @param var2 time point
     * @return lower bound on time between time point var1 and var2
     */
    protected int ga(int var1, int var2){
        if(var1 == var2) return 0;
        else if(var1 < var2) return (- gb(var2,var1));
        else return edge_a[FullSTN.precalc[var1][var2]];
    }
    /**
     * returns the upper bound on time between time point var1 and var2, uses symmetry of the network
     * @param var1 time point
     * @param var2 time point
     * @return lower bound on time between time point var1 and var2
     */
    protected int gb(int var1, int var2){
        if(var1 == var2) return 0;
        else if(var1 < var2) return (- ga(var2,var1));
        else return edge_b[FullSTN.precalc[var1][var2]];
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
    protected void sa(int var1, int var2, int value){
        if(var1 < var2) sb(var2,var1, - value);
        else edge_a[var1*(var1-1)/2 + var2] = value;
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
    protected void sb(int var1, int var2, int value){
        if(var1 < var2) sa(var2,var1, - value);
        else edge_b[var1*(var1-1)/2 + var2] = value;
    }

    /**
     * adds new time point into the network. In fact adds only edges and sets them to (inf,sup)
     * @return id of the newly inserted time point
     */
    protected int add_v(){
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
        if(top != 0)
            eless(0,top);
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
    protected boolean edge_consistent(int v1, int v2, int a, int b){
        //non-empty intersection
        return (Math.max(ga(v1,v2),a) <= Math.min(gb(v1,v2), b));
    }
    /**
     * addition operation that takes into account our infities
     * @param a time
     * @param b time
     * @return product of addition operation on a and b
     */
    protected int lim_plus(int a, int b){
        return (a == inf || b == inf)?inf:(a == sup || b == sup)?sup:a+b;
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
    protected void propagate(int v1, int v2, int a, int b){
        if(!consistent || !edge_consistent(v1, v2, a, b)) {
            consistent = false;
            return;
        }

        int I_top = 0, J_top = 0, I[] = new int[top], J[] = new int[top], aa, bb, i,j,k;

        aa = Math.max(a, ga(v1,v2)); //(K->V1 + V1->V2) /\ (K->V2)
        bb = Math.min(b, gb(v1,v2));
        if(ga(v1,v2) != aa || gb(v1,v2) != bb){

            // intersection of the original edge
            sa(v1, v2, aa);
            sb(v1, v2, bb);

            // propagation to all connected edges
            for(k = 0; k < top; k++) {
                if(emptySpots.contains(k)) break;
                if (v1 != k && v2 != k) {
                    aa = lim_plus(ga(k, v1), ga(v1, v2)); //K->V1 + V1->V2
                    bb = lim_plus(gb(k, v1), gb(v1, v2));
                    aa = Math.max(aa, ga(k, v2)); //(K->V1 + V1->V2) /\ (K->V2)
                    bb = Math.min(bb, gb(k, v2));
                    if (ga(k, v2) != aa || gb(k, v2) != bb) {//change of interval occured
                        I[I_top++] = k;
                        sa(k, v2, aa);
                        sb(k, v2, bb);
                    }
                    aa = lim_plus(ga(v1, v2), ga(v2, k)); //V1->V2 + V2->K
                    bb = lim_plus(gb(v1, v2), gb(v2, k));
                    aa = Math.max(aa, ga(v1, k)); //(V1->V2 + V2->K) /\ (V1->K)
                    bb = Math.min(bb, gb(v1, k));
                    if (ga(v1, k) != aa || gb(v1, k) != bb) {//change of interval occured
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
                        aa = lim_plus(ga(I[i], v1), ga(v1, J[j])); //I->V1 + V1->J
                        bb = lim_plus(gb(I[i], v1), gb(v1, J[j]));
                        aa = Math.max(aa, ga(I[i], J[j])); //(I->V1 + V1->J) /\ (I->J)
                        bb = Math.min(bb, gb(I[i], J[j]));
                        if (ga(I[i], J[j]) != aa || gb(I[i], J[j]) != bb) {
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
        return consistent;
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
        enforceBefore(start(), u);
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
    public IList<Tuple5<Object, Object, Object, Enumeration.Value, Option<ID>>> constraints() {
        IList<Tuple5<Object, Object, Object, Enumeration.Value, Option<ID>>> list = new IList<>();
        for(Tuple4<Integer,Integer,Integer,ID> cons : allConstraints) {
            Tuple5<Object, Object, Object, Enumeration.Value, Option<ID>> current;
            if(cons._4() != null)
                current = new Tuple5<>((Object) cons._1(), (Object) cons._2(), (Object) cons._3(), ElemStatus.CONTROLLABLE(), (Option<ID>) new Some<>(cons._4()));
            else
                current = new Tuple5<>((Object) cons._1(), (Object) cons._2(), (Object) cons._3(), ElemStatus.CONTROLLABLE(), (Option<ID>) None$.empty());
            list = list.with(current);
        }
        return list;
    }

    /*
        @Override
        public IList<Tuple5<Object, Object, Object, Enumeration.Value>> constraints() {
            List<Tuple5<Object, Object, Object, Enumeration.Value>> all = new LinkedList<>();

        }
    */
    @Override
    public int size() {
        return this.top - emptySpots.size();
    }

    @Override
    public boolean addConstraint(int u, int v, int w) {
        allConstraints = allConstraints.with(new Tuple4<Integer, Integer, Integer, ID>(u, v, w, null));
        propagate(u, v, inf, w);
        return consistent();
    }

    @Override
    public boolean addConstraintWithID(int u, int v, int w, ID o) {
        allConstraints = allConstraints.with(new Tuple4<Integer, Integer, Integer, ID>(u, v, w, o));
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
        return ga(start(), u);
    }

    @Override
    public int latestStart(int u) {
        return gb(start(), u);
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
