/*
 * This file is part of Filuta.
 *
 * Filuta  - AI planning system with time and resources
 *
 * Author: Filip Dvořák (filip.dvorak@runbox.com)
 * (C) Copyright 2008-2009 Filip Dvořák
 *
 */
package fape.core.planning.stn;

/**
 * Same as Simple Temporal Network, except no time points are added, hence no
 * preallocation is needed. To speed up processing, time points of the
 * subnetwork are initially mapped to time points of the original network. The
 * mapping is static (there is never a need to store multiple subnetworks).
 *
 * @author Filip Dvořák
 */
class TinySTN extends STN {

    static int mapping_STN_to_TinySTN[], mapping_TinySTN_to_STN[];

    /**
     * constructs new subnetwork and creates initial mapping of time points
     *
     * @param s original network
     * @param time_points set of concerned time points
     */
    public TinySTN(STN s, boolean[] time_points) {
        int size = 0;
        mapping_STN_to_TinySTN = new int[s.top];
        mapping_TinySTN_to_STN = new int[s.top]; //could be smaller with one more iteration, solved later
        for (int i = 0; i < time_points.length; i++) {
            if (time_points[i]) {
                mapping_STN_to_TinySTN[i] = size;
                mapping_TinySTN_to_STN[size] = i;
                size++;
            }
        }
        int[] pm = new int[size];
        System.arraycopy(mapping_TinySTN_to_STN, 0, pm, 0, size);
        mapping_TinySTN_to_STN = pm;

        int edges_needed = (size * size - size) / 2;
        this.edge_a = new int[edges_needed];
        this.edge_b = new int[edges_needed];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < i; j++) {
                this.sa(mapping_TinySTN_to_STN[i], mapping_TinySTN_to_STN[j], s.ga(mapping_TinySTN_to_STN[i], mapping_TinySTN_to_STN[j]));
                this.sb(mapping_TinySTN_to_STN[i], mapping_TinySTN_to_STN[j], s.gb(mapping_TinySTN_to_STN[i], mapping_TinySTN_to_STN[j]));
            }
        }
        this.top = size;
    }

    /**
     * copy constructor without preallocation
     *
     * @param s
     */
    public TinySTN(TinySTN s) {
        this.top = s.top;
        edge_a = new int[s.edge_a.length];
        edge_b = new int[s.edge_b.length];
        System.arraycopy(s.edge_a, 0, edge_a, 0, top * (top - 1) / 2);
        System.arraycopy(s.edge_b, 0, edge_b, 0, top * (top - 1) / 2);
    }

    @Override
    public final int ga(int var1, int var2) {
        if (var1 < var2) {
            return (-gb(var2, var1));
        } else {
            return edge_a[mapping_STN_to_TinySTN[var1] * (mapping_STN_to_TinySTN[var1] - 1) / 2 + mapping_STN_to_TinySTN[var2]]; //using sum
        }
    }

    @Override
    public final int gb(int var1, int var2) {
        if (var1 < var2) {
            return (-ga(var2, var1));
        } else {
            return edge_b[mapping_STN_to_TinySTN[var1] * (mapping_STN_to_TinySTN[var1] - 1) / 2 + mapping_STN_to_TinySTN[var2]];
        }
    }

    @Override
    public final void sa(int var1, int var2, int value) {
        if (var1 < var2) {
            sb(var2, var1, -value);
        } else {
            edge_a[mapping_STN_to_TinySTN[var1] * (mapping_STN_to_TinySTN[var1] - 1) / 2 + mapping_STN_to_TinySTN[var2]] = value;
        }
    }

    @Override
    public final void sb(int var1, int var2, int value) {
        if (var1 < var2) {
            sa(var2, var1, -value);
        } else {
            edge_b[mapping_STN_to_TinySTN[var1] * (mapping_STN_to_TinySTN[var1] - 1) / 2 + mapping_STN_to_TinySTN[var2]] = value;
        }
    }

    @Override
    public void propagate(int v1, int v2, int a, int b) {
        int I_top = 0, J_top = 0, I[] = new int[top], J[] = new int[top], aa, bb, i, j, k;

        aa = Math.max(a, ga(v1, v2)); //(K->V1 + V1->V2) /\ (K->V2)
        bb = Math.min(b, gb(v1, v2));
        if (ga(v1, v2) != aa || gb(v1, v2) != bb) {

            // intersection of the original edge
            sa(v1, v2, aa);
            sb(v1, v2, bb);

            // propagation to all connected edges
            for (k = 0; k < top; k++) {
                if (v1 != mapping_TinySTN_to_STN[k] && v2 != mapping_TinySTN_to_STN[k]) {
                    aa = lim_plus(ga(mapping_TinySTN_to_STN[k], v1), ga(v1, v2)); //K->V1 + V1->V2
                    bb = lim_plus(gb(mapping_TinySTN_to_STN[k], v1), gb(v1, v2));
                    aa = Math.max(aa, ga(mapping_TinySTN_to_STN[k], v2)); //(K->V1 + V1->V2) /\ (K->V2)
                    bb = Math.min(bb, gb(mapping_TinySTN_to_STN[k], v2));
                    if (ga(mapping_TinySTN_to_STN[k], v2) != aa || gb(mapping_TinySTN_to_STN[k], v2) != bb) {//change of interval occured
                        I[I_top++] = k;
                        sa(mapping_TinySTN_to_STN[k], v2, aa);
                        sb(mapping_TinySTN_to_STN[k], v2, bb);
                    }
                    aa = lim_plus(ga(v1, v2), ga(v2, mapping_TinySTN_to_STN[k])); //V1->V2 + V2->K
                    bb = lim_plus(gb(v1, v2), gb(v2, mapping_TinySTN_to_STN[k]));
                    aa = Math.max(aa, ga(v1, mapping_TinySTN_to_STN[k])); //(V1->V2 + V2->K) /\ (V1->K)
                    bb = Math.min(bb, gb(v1, mapping_TinySTN_to_STN[k]));
                    if (ga(v1, mapping_TinySTN_to_STN[k]) != aa || gb(v1, mapping_TinySTN_to_STN[k]) != bb) {//change of interval occured
                        J[J_top++] = k;
                        sa(v1, mapping_TinySTN_to_STN[k], aa);
                        sb(v1, mapping_TinySTN_to_STN[k], bb);
                    }
                }
            }

            for (i = 0; i < I_top; i++) {
                for (j = 0; j < J_top; j++) {
                    if (I[i] != J[j]) { //!! this is not semanticly correct, however mapping is uniquely invertible
                        aa = lim_plus(ga(mapping_TinySTN_to_STN[I[i]], v1), ga(v1, mapping_TinySTN_to_STN[J[j]])); //I->V1 + V1->J
                        bb = lim_plus(gb(mapping_TinySTN_to_STN[I[i]], v1), gb(v1, mapping_TinySTN_to_STN[J[j]]));
                        aa = Math.max(aa, ga(mapping_TinySTN_to_STN[I[i]], mapping_TinySTN_to_STN[J[j]])); //(I->V1 + V1->J) /\ (I->J)
                        bb = Math.min(bb, gb(mapping_TinySTN_to_STN[I[i]], mapping_TinySTN_to_STN[J[j]]));
                        if (ga(mapping_TinySTN_to_STN[I[i]], mapping_TinySTN_to_STN[J[j]]) != aa || gb(mapping_TinySTN_to_STN[I[i]], mapping_TinySTN_to_STN[J[j]]) != bb) {
                            sa(mapping_TinySTN_to_STN[I[i]], mapping_TinySTN_to_STN[J[j]], aa);
                            sb(mapping_TinySTN_to_STN[I[i]], mapping_TinySTN_to_STN[J[j]], bb);
                        }
                    }
                }
            }
        }
    }
}
