/*
 * Author:  Filip Dvořák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvořák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */

package fape.core.planning.heuristics.lmcut;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class RelaxedGroundAction {
    List<RelaxedGroundAtom> pre = new LinkedList<>(), eff = new LinkedList<>();
    String name;

    @Override
    public String toString() {
        return name; //To change body of generated methods, choose Tools | Templates.
    }

    public RelaxedGroundAtom hMaxSupporter;
    public float hMaxVal;
    
    public void ResetHmax(){
        hMaxSupporter = null;
        hMaxVal = Float.POSITIVE_INFINITY;
    }

    public boolean IsApplicableClean(HashSet<RelaxedGroundAtom> kb) {
        boolean applicable = true;
        for (RelaxedGroundAtom a : pre) {
            if (!kb.contains(a)) {
                applicable = false;
            }
        }
        return applicable;
    }
    public float cost = 1;
}
