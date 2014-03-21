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

package fape.core.planning.constraints;

import planstack.anml.model.concrete.VarRef;

/**
 *
 * @author FD
 */
public class UnificationConstraint {
    public final VarRef one, two;

    /**
     * Creates a new Unification constraint stating that a == b
     */
    public UnificationConstraint(VarRef a, VarRef b) {
        one = a;
        two = b;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof UnificationConstraint) {
            UnificationConstraint u = (UnificationConstraint) obj;
            return (u.one.equals(one) && u.two.equals(two)) || (u.one.equals(two) && u.two.equals(one));
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + one.hashCode() + two.hashCode();
        return hash;
    }
}
