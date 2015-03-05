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

import java.util.HashMap;

/**
 *
 * @author FD
 */
public class RelaxedGroundAtom {
    
    public static boolean Indexed(String name){
        return indexer.containsKey(name);
    }
    
    public static void ResetIndexes() {
        indexer = new HashMap<>();
        inverseIndexer = new HashMap<>();
        counter = 0;
    }

    public void ReIndex() {
        if (!indexer.containsKey(mName)) {
            indexer.put(mName, counter);
            inverseIndexer.put(counter, mName);
            counter++;
        }
        mID = indexer.get(mName);
    }

    public RelaxedGroundAtom(String name) {
        mName = name;
        ReIndex();
    }

    private static HashMap<String, Integer> indexer = new HashMap<>();
    private static HashMap<Integer, String> inverseIndexer = new HashMap<>();
    private static int counter = 0;

    public static String GetAtomName(int i) {
        return inverseIndexer.get(i);
    }

    public int mID = -1;

    public String mName;

    @Override
    public String toString() {
        return mName;
    }

    @Override
    public boolean equals(Object obj) {
        return ((RelaxedGroundAtom) obj).mID == mID;
        //return ((Atom)obj).mName.equals(mName);        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + mID;//Objects.hashCode(this.mName);
        return hash;
    }
}
