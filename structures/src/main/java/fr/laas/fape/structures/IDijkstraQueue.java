package fr.laas.fape.structures;

import java.util.Arrays;

public class IDijkstraQueue<E> {

    private final IntRep<E> rep;
    private int currentSize; // Number of elements in heap
    private IR2IntMap<E> costs;
    private final IR2IntMap<E> positions;

    private int[] array; // The heap array

    /**
     * Construct the binary heap.
     */
    public IDijkstraQueue(IntRep<E> rep) {
        this.rep = rep;
        this.currentSize = 0;
        this.array = new int[50];
        Arrays.fill(this.array, -1);
        this.costs = new IR2IntMap<E>(rep);
        this.positions = new IR2IntMap<E>(rep);
    }

    private IDijkstraQueue(IDijkstraQueue<E> q) {
        rep = q.rep;
        currentSize = q.currentSize;
        array = Arrays.copyOf(q.array, q.array.length);
        costs = q.costs.clone();
        positions = q.positions.clone();
    }

    public void initCosts(IR2IntMap<E> costs) {
        assert this.costs.isEmpty();
        this.costs = costs.clone();
    }
    public IR2IntMap<E> getCosts() { return this.costs; }

    private int compare(int id1, int id2) {
        return costs.get(id1) - costs.get(id2);
    }

    // Sets an element in the array
    private void arraySet(int index, int valueID) {
        if(index >= array.length) {
            int[] oldArray = array;
            this.array = Arrays.copyOf(oldArray, Math.max(oldArray.length*2, index + 1));
            Arrays.fill(this.array, oldArray.length, array.length, -1);
        }
        array[index] = valueID;
        positions.put(valueID, index);
    }

    public boolean isEmpty() { return this.currentSize == 0; }
    public int size() { return this.currentSize; }

    /** Returns index of parent. */
    private int index_parent(int index) { return (index - 1) / 2; }

    /** Returns index of left child. */
    private int index_left(int index) { return index * 2 + 1; }

    /**
     * Insert into the heap.
     *
     * @param x the item to insert.
     */
    public final void insert(E x, int cost) {
        insert(rep.asInt(x), cost);
    }
    public final void insert(int id, int cost) {
        assert !contains(id);
        int index = this.currentSize++;
        this.costs.put(id, cost);
        this.arraySet(index, id);
        this.percolateUp(index);
    }

    public final void update(E x, int cost) {
        update(rep.asInt(x), cost);
    }

    public final void update(int id, int cost) {
        assert contains(id);
        this.costs.put(id, cost);
        int index = positions.get(id);
        percolateDown(index);
        percolateUp(index);
    }

    public boolean contains(E x) { return contains(rep.asInt(x)); }
    public boolean contains(int id) { return positions.containsKey(id); }
    public boolean hasCost(E x) { return hasCost(rep.asInt(x)); }
    public boolean hasCost(int id) { return costs.containsKey(id); }
    public int getCost(E x) { return getCost(rep.asInt(x)); }
    public int getCost(int id) { return costs.get(id); }

    /**
     * Internal method to percolate up in the heap.
     *
     * @param index the index at which the percolate begins.
     */
    private void percolateUp(int index) {
        int x = this.array[index];

        for (; index > 0 && compare(x, array[index_parent(index)]) < 0; index = index_parent(index)) {
            int moving_val = array[index_parent(index)];
            this.arraySet(index, moving_val);
        }

        this.arraySet(index, x);
    }

    /**
     * Internal method to percolate down in the heap.
     *
     * @param index the index at which the percolate begins.
     */
    private void percolateDown(int index) {
        int ileft = index_left(index);
        int iright = ileft + 1;

        if (ileft < this.currentSize) {
            int current = array[index];
            int left = array[ileft];
            boolean hasRight = iright < this.currentSize;
            int right = (hasRight) ? array[iright] : -1;

            if (!hasRight || compare(left, right) < 0) {
                // Left is smaller
                if (compare(left, current) < 0) {
                    this.arraySet(index, left);
                    this.arraySet(ileft, current);
                    this.percolateDown(ileft);
                }
            } else {
                // Right is smaller
                if (compare(right, current) < 0) {
                    this.arraySet(index, right);
                    this.arraySet(iright, current);
                    this.percolateDown(iright);
                }
            }
        }
    }

    /**
     * Find the smallest item in the heap.
     *
     * @return the smallest item.
     * @throws Exception if empty.
     */
    private int findMin() {
        assert !isEmpty() : "Heap is empty";
        return array[0];
    }

    /**
     * Remove the smallest item from the heap.
     *
     * @return the smallest item.
     * @throws Exception if empty.
     */
    public E poll() {
        return rep.fromInt(pollId());
    }
    public int pollId() {
        assert !isEmpty() : "Heap is empty.";
        int minItem = findMin();
        int lastItem = array[--this.currentSize];
        arraySet(0, lastItem);
        percolateDown(0);
        positions.remove(minItem);
        return minItem;
    }


    public void cleanup(E x) {
        cleanup(rep.asInt(x));
    }
    public void cleanup(int id) {
        if(contains(id))
            removeAllTraces(id);
        else if(hasCost(id))
            costs.remove(id);
    }

    private void removeAllTraces(int id) {
        int index = positions.get(id);
        int lastItem = array[--this.currentSize];
        arraySet(index, lastItem);
        percolateDown(index);
        percolateUp(index);
        positions.remove(id);
        costs.remove(id);
    }

    /**
     * Prints the heap
     */
    public void print() {
        System.out.println();
        System.out.println("========  HEAP  (size = " + this.currentSize + ")  ========");
        System.out.println();

        for (int i = 0; i < this.currentSize; i++) {
            System.out.println(costs.get(array[i])+" "+ rep.fromInt(array[i]).toString());
        }

        System.out.println();
        System.out.println("--------  End of heap  --------");
        System.out.println();
    }

    /**
     * Prints the elements of the heap according to their respective order.
     */
    public void printSorted() {

        IDijkstraQueue<E> copy = new IDijkstraQueue<>(this);

        System.out.println();
        System.out.println("========  Sorted HEAP  (size = " + this.currentSize + ")  ========");
        System.out.println();

        while (!copy.isEmpty()) {
            int next = copy.findMin();
            System.out.println(costs.get(next)+" "+rep.fromInt(next));
        }

        System.out.println();
        System.out.println("--------  End of heap  --------");
        System.out.println();
    }

}
