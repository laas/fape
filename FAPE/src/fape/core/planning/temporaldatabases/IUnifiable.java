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
package fape.core.planning.temporaldatabases;

import java.util.HashSet;
import java.util.List;

/**
 *
 * @author FD
 */
public abstract class IUnifiable {

    public static int idCounter = 0;
    public int mID = -1;

    public abstract List<String> GetDomainObjectConstants();

    /**
     * reduces the domain, if elements were removed, returns true
     *
     * @param supported
     * @return
     */
    public abstract boolean ReduceDomain(HashSet<String> supported);

    public abstract int GetUniqueID();

    public abstract boolean EmptyDomain();

    public abstract String Explain();
    
    @Override
    public String toString(){
        return GetDomainObjectConstants().toString();
        
    }
    
    
}
