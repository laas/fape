package fr.laas.fape.structures;

import java.util.*;

public class DijkstraQueue<E> {

    private int currentSize; // Number of elements in heap
    private Map<E,Integer> costs;
    private Map<E,Integer> costsToGo;
    private final Map<E,Integer> positions;
    private final Map<E,E> predecessors;

    private E[] array; // The heap array

    /**
     * Construct the binary heap.
     */
    public DijkstraQueue() {
        this.currentSize = 0;
        this.array = (E[]) new Object[50];
        Arrays.fill(this.array, -1);
        this.costs = new HashMap<>();
        this.costsToGo = new HashMap<>();
        this.positions = new HashMap<>();
        this.predecessors = new HashMap<>();
    }

    private DijkstraQueue(DijkstraQueue<E> q) {
        currentSize = q.currentSize;
        array = Arrays.copyOf(q.array, q.array.length);
        costs = new HashMap<>(q.costs);
        costsToGo = new HashMap<>(q.costsToGo);
        positions = new HashMap<>(q.positions);
        predecessors = new HashMap<>(q.predecessors);
    }

    private int compare(E e1, E e2) {
        int c1 = e1 == null ? -1 : costs.get(e1) + costsToGo.get(e1);
        int c2 = e2 == null ? -1 : costs.get(e2) + costsToGo.get(e2);
        return c1 - c2;
    }

    // Sets an element in the array
    private void arraySet(int index, E value) {
        if(index >= array.length) {
            E[] oldArray = array;
            this.array = Arrays.copyOf(oldArray, Math.max(oldArray.length*2, index + 1));
            Arrays.fill(this.array, oldArray.length, array.length, -1);
        }
        array[index] = value;
        positions.put(value, index);
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
    public final void insert(E x, int cost, int costToGo, E predecessor) {
        assert !contains(x);
        int index = this.currentSize++;
        this.costs.put(x, cost);
        this.costsToGo.put(x, costToGo);
        this.arraySet(index, x);
        this.percolateUp(index);
        if(predecessor != null)
            this.predecessors.put(x, predecessor);
    }

    public final void update(E x, int cost, int costToGo, E predecessor) {
        assert contains(x);
        this.costs.put(x, cost);
        this.costsToGo.put(x, costToGo);
        int index = positions.get(x);
        percolateDown(index);
        percolateUp(index);
        if(predecessor != null)
            this.predecessors.put(x, predecessor);
    }

    public final void putIfBetter(E x, int cost, int costToGo, E predecessor) {
        if(hasCost(x) && getCost(x) < cost) {
            return;
        } else if(contains(x)) {
            update(x, cost, costToGo, predecessor);
        } else {
            insert(x, cost, costToGo, predecessor);
        }
    }

    public boolean contains(E x) { return positions.containsKey(x); }
    public boolean hasCost(E x) { return costs.containsKey(x); }
    public int getCost(E x) { return costs.get(x); }
    public int getCostToGo(E x) { return costsToGo.get(x); }
    public E getPredecessor(E x) { return predecessors.get(x); }
    public List<E> getPathTo(E x) {
        List<E> l = predecessors.containsKey(x) ? getPathTo(predecessors.get(x)) : new ArrayList<>();
        l.add(x);
        return l;
    }

    /**
     * Internal method to percolate up in the heap.
     *
     * @param index the index at which the percolate begins.
     */
    private void percolateUp(int index) {
        E x = this.array[index];

        for (; index > 0 && compare(x, array[index_parent(index)]) < 0; index = index_parent(index)) {
            E moving_val = array[index_parent(index)];
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
            E current = array[index];
            E left = array[ileft];
            boolean hasRight = iright < this.currentSize;
            E right = (hasRight) ? array[iright] : null;

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
    private E findMin() {
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
        assert !isEmpty() : "Heap is empty.";
        E minItem = findMin();
        E lastItem = array[--this.currentSize];
        arraySet(0, lastItem);
        percolateDown(0);
        positions.remove(minItem);
        return minItem;
    }


    public void cleanup(E x) {
        if(contains(x))
            removeAllTraces(x);
        else if(hasCost(x))
            costs.remove(x);
    }

    private void removeAllTraces(E e) {
        int index = positions.get(e);
        E lastItem = array[--this.currentSize];
        arraySet(index, lastItem);
        percolateDown(index);
        percolateUp(index);
        positions.remove(e);
        costs.remove(e);
    }

    /**
     * Prints the heap
     */
    public void print() {
        System.out.println();
        System.out.println("========  HEAP  (size = " + this.currentSize + ")  ========");
        System.out.println();

        for (int i = 0; i < this.currentSize; i++) {
            System.out.println(costs.get(array[i])+" "+ array[i].toString());
        }

        System.out.println();
        System.out.println("--------  End of heap  --------");
        System.out.println();
    }

    /**
     * Prints the elements of the heap according to their respective order.
     */
    public void printSorted() {

        DijkstraQueue<E> copy = new DijkstraQueue<>(this);

        System.out.println();
        System.out.println("========  Sorted HEAP  (size = " + this.currentSize + ")  ========");
        System.out.println();

        while (!copy.isEmpty()) {
            E next = copy.findMin();
            System.out.println(costs.get(next)+" "+next);
        }

        System.out.println();
        System.out.println("--------  End of heap  --------");
        System.out.println();
    }

}
