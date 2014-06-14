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
package fape.core.planning.search.strategies.plans;

import fape.core.planning.heuristics.lmcut.LMCut;
import fape.core.planning.heuristics.lmcut.RelaxedGroundAtom;
import fape.core.planning.planner.APlanner;
import fape.core.planning.planninggraph.GroundProblem;
import fape.core.planning.search.SupportOption;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.VarRef;

/**
 *
 * @author FD
 */
public class LMC implements PartialPlanComparator {

    /**
     * gets an atom description if fully binded, returns null otherwise
     *
     * @param state
     * @param v
     * @param value
     * @return
     */
    public static String GetAtomName(State state, ParameterizedStateVariable v, VarRef value) {
        Collection<String> dom = state.conNet.domainOf(value);
        if (dom.size() > 1) {
            return null; //checking only the fully binded variables
        }
        String val = null;
        for (String st : dom) {
            val = st;
            break;
        }
        String name = v.func().name() + "[";
        scala.collection.Iterator<VarRef> it = v.args().iterator();
        while (it.hasNext()) {
            VarRef var = it.next();
            Collection<String> dm = state.conNet.domainOf(var);
            if (dm.size() > 1) {
                return null; //checking only the fully binded variables
            }
            String vl = "";
            for (String st : dm) {
                vl = st;
                break;
            }
            name += vl + ",";
        }
        name = name.substring(0, name.length() - 1);
        name += "]=" + val;
        return name;
    }

    private class Slice {

        String variable;
        String value;
        String atom;
        TPRef when;

        public Slice(String at, TPRef ref) {
            atom = at;
            String ar[] = at.split("=");
            variable = ar[0];
            value = ar[1];
            when = ref;
        }

        @Override
        public String toString() {
            return atom; //To change body of generated methods, choose Tools | Templates.
        }

    }

    public void Evaluate(State st, LMCut lm) {
        BitSet init = new BitSet(), goal = new BitSet();
        st.g = st.taskNet.GetAllActions().size();
        //now we need to translate consumers into goals, we are interested in those consumers that require an action application

        HashMap<String, Slice> goalSlice = new HashMap<>(), initSlice = new HashMap<>();

        for (TemporalDatabase b : st.consumers) {
            boolean hasSimpleResolution = false;
            List<SupportOption> ops = APlanner.currentPlanner.GetSupporters(b, st);
            for (SupportOption o : ops) {
                if (o.representsCausalLinkAddition()) {
                    hasSimpleResolution = true;
                }
            }
            if (!hasSimpleResolution) {
                VarRef v = b.GetGlobalConsumeValue();
                String name = GetAtomName(st, b.stateVariable, v);
                RelaxedGroundAtom at = new RelaxedGroundAtom(name);
                if (name != null) {
                    goal.set(at.mID);
                    Slice s = new Slice(name, b.getConsumeTimePoint());
                    goalSlice.put(s.variable, s);
                }

            }
        }
        //now we make a slice of the initial state we are considering
        if (LMCut.commonInit == null) {
            for (TemporalDatabase b : st.tdb.vars) {
                if (b.HasSinglePersistence()) {
                    continue;
                }
                String nm = b.GetRepresentativeSupportAtomName(st);
                if (nm != null && RelaxedGroundAtom.Indexed(nm)) {
                    Slice s = new Slice(nm, b.getConsumeTimePoint());
                    Slice ss = goalSlice.get(s.variable);
                    Slice sss = initSlice.get(s.variable);
                    //add only those slices to the init that can be ordered vefore the corresponding goal slice

                    if ((ss == null || !st.tempoNet.CanBeBefore(ss.when, s.when))
                            && (sss == null || st.tempoNet.CanBeBefore(sss.when, s.when))) {
                        initSlice.put(s.variable, s);
                    }
                }
            }
            for (Slice s : initSlice.values()) {
                RelaxedGroundAtom at = new RelaxedGroundAtom(s.atom);
                init.set(at.mID);
            }
            LMCut.commonInit = init;
        }

        st.h = lm.Eval(LMCut.commonInit, goal);

    }
    
    public static LMCut lmc = null;

    public float cost(State st) {
        if (st.g == -1 && st.h == -1) {
            if(lmc == null){
                lmc = new LMCut(new GroundProblem(st.pb));
            }
            Evaluate(st, lmc);
        }
        return st.g + st.h;
    }

    @Override
    public String shortName() {
        return "lmc";
    }

    @Override
    public int compare(State o1, State o2) {
        return (int) (cost(o1) - cost(o2));
    }

}
