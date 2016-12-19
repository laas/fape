package fr.laas.fape.graph;

import fr.laas.fape.graph.core.*;

/**
 * Provides factory methods for the default implementations of the graphs.
 */
public class GraphFactory {

    /**
     *
     * @param <V> Type of vertices
     * @param <EL> Type of the edges' labels
     * @return A new Labeled directed graph.
     */
    public static <V,EL> LabeledDigraph<V,EL> getLabeledDigraph() {
        return LabeledDigraph$.MODULE$.apply();
    }

    /**
     *
     * @param <V> Type of vertices
     * @return A new directed graph with no label on its edges.
     */
    public static <V> UnlabeledDigraph<V> getUnlabeledDigraph() {
        return UnlabeledDigraph$.MODULE$.apply();
    }

    /**
     *
     * @param <V> Type of vertices
     * @param <EL> Type of the edges' labels
     * @return A new Labeled directed simple graph.
     */
    public static <V, EL> SimpleLabeledDigraph<V,EL> getSimpleLabeledDigraph() {
        return SimpleLabeledDigraph$.MODULE$.apply();
    }

    /**
     *
     * @param <V> Type of vertices
     * @param <EL> Type of the edges' labels
     * @return A new Labeled directed multi graph.
     */
    public static <V, EL> MultiLabeledDigraph<V,EL> getMultiLabeledDigraph() {
        return MultiLabeledDigraph$.MODULE$.apply();
    }

    /**
     *
     * @param <V> Type of vertices
     * @return A new directed simple graph with no label on its edges.
     */
    public static <V> SimpleUnlabeledDigraph<V> getSimpleUnlabeledDigraph() {
        return SimpleUnlabeledDigraph$.MODULE$.apply();
    }

    /**
     *
     * @param <V> Type of vertices
     * @return A new directed multi graph with no label on its edges.
     */
    public static <V> MultiUnlabeledDigraph<V> getMultiUnlabeledDigraph() {
        return MultiUnlabeledDigraph$.MODULE$.apply();
    }




}
