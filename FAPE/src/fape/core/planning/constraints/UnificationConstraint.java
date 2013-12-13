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

import fape.core.planning.temporaldatabases.IUnifiable;
import fape.core.planning.temporaldatabases.TemporalDatabase;

/**
 *
 * @author FD
 */
public class UnificationConstraint {
    IUnifiable one, two;

        @Override
        public boolean equals(Object obj) {
            UnificationConstraint u = (UnificationConstraint) obj;
            return (u.one == one && u.two == two) || (u.one == two && u.two == one);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + this.one.mID + this.two.mID;
            return hash;
        }

        public UnificationConstraint(IUnifiable f, IUnifiable s) {
            one = f;
            two = s;
        }
}
