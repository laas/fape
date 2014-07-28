/*
 * Author:  Filip Dvorak <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvorak <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.planning.search.strategies.flaws;

import fape.core.planning.search.Flaw;
import fape.core.planning.search.ResourceFlaw;
import fape.core.planning.search.resolvers.Resolver;
import fape.util.Pair;

import java.util.List;

/**
 *
 * @author FD
 */
public class ResourceFlawPreference implements FlawComparator {

    @Override
    public int compare(Pair<Flaw, List<Resolver>> f1, Pair<Flaw, List<Resolver>> f2) {
        if(f1.value1 instanceof ResourceFlaw){
            if(f2.value1 instanceof ResourceFlaw){
                return 0;
            }else{
                return -1;
            }
        }else if(f2.value1 instanceof ResourceFlaw){
            return 1;
        }else{
            return 0;
        }
    }

    @Override
    public String shortName() {
        return "rfp";
    }
}
