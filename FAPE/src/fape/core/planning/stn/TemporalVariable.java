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
package fape.core.planning.stn;

/**
 *
 * @author FD
 */
public class TemporalVariable {

    private static int idCounter = 0;
    private final int mID = idCounter++;

    
    
    @Override
    public boolean equals(Object obj) {
        return mID == ((TemporalVariable) (obj)).mID;
    }

    public boolean equals(TemporalVariable obj) {
        return mID == obj.mID;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.mID;
        return hash;
    }

    public int getID() {
        return mID;
    }
}
