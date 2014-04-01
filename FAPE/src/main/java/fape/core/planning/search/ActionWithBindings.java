package fape.core.planning.search;


import planstack.anml.model.concrete.Action;
import planstack.anml.model.concrete.VarRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ActionWithBindings {
    public Action act;
    public Map<VarRef, Collection<String>> values = new HashMap<>();
}