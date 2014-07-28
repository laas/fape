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

package fape.core.planning.search;

import fape.core.planning.search.resolvers.Resolver;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author FD
 */
public class ResourceFlaw extends Flaw {
    public List<Resolver> resolvers = new LinkedList<>();
}
