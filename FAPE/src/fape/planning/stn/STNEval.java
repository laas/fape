/*
 * This file is part of Filuta.
 *
 * Filuta  - AI planning system with time and resources
 *
 * Author: Filip Dvořák (filip.dvorak@runbox.com)
 * (C) Copyright 2008-2009 Filip Dvořák
 *
 */

package fape.planning.stn;

/**
 * Pair of ints, represents product of evaluation of a state, provides comparison.
 * @author Filip Dvořák
 */
public class STNEval {
    int a,b;
    public STNEval(int a, int b){
        this.a = a; this.b = b;
    }
    public static boolean smaller(STNEval x, STNEval y){
        return (x.a < y.a)?true:(x.a == y.a && x.b < y.b)?true:false;
    }
}
