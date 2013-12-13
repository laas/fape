/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    public int mID;
    public abstract List<String> GetDomainObjectConstants();
    public abstract boolean ReduceDomain(HashSet<String> supported);
    public abstract int GetUniqueID();
}
