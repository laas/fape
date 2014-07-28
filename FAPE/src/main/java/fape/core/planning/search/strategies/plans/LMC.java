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
import fape.core.planning.search.resolvers.Resolver;
import fape.core.planning.states.State;
import fape.core.planning.temporaldatabases.TemporalDatabase;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.ParameterizedStateVariable;
import planstack.anml.model.concrete.TPRef;
import planstack.anml.model.concrete.VarRef;

import java.util.*;

/**
 *
 * @author FD
 */
public class LMC implements PartialPlanComparator {

    public static LMC singleton = null;

    private static void recGen(List<Collection<String>> list, List<String> record, String partial, int level) {
        if (level == list.size()) {
            record.add(partial);
            return;
        }
        Collection<String> col = list.get(level);
        for (String s : col) {
            recGen(list, record, partial + s + ",", level + 1);
        }
    }

    public static List<String> GetAtomNames(State state, ParameterizedStateVariable v, VarRef value) {
        List<String> ret = new ArrayList<>();
        Collection<String> dom = state.conNet.domainOf(value);
        for (String val : dom) {
            List<Collection<String>> inits = new LinkedList<>();
            scala.collection.Iterator<VarRef> it = v.args().iterator();
            while (it.hasNext()) {
                VarRef var = it.next();
                inits.add(state.conNet.domainOf(var));
            }
            List<String> variations = new LinkedList<>();
            recGen(inits, variations, "", 0);
            for (String s : variations) {
                String name = v.func().name() + "[" + s;
                name = name.substring(0, name.length() - 1);
                name += "]=" + val;
                ret.add(name);
            }
        }
        return ret;
    }

    /**
     * gets an atom description if fully binded, returns null otherwise
     *
     * @param state
     * @param v
     * @param value
     * @return
     */
    /*public static String GetAtomName(State state, ParameterizedStateVariable v, VarRef value) {
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
     }*/
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

    public LMC() {
        singleton = this;
    }

    public void Evaluate(State st, LMCut lm) {
        BitSet init = new BitSet(), goal = new BitSet();
        st.g = st.taskNet.GetAllActions().size();
        //now we need to translate consumers into goals, we are interested in those consumers that require an action application

        HashMap<String, Slice> goalSlice = new HashMap<>(), initSlice = new HashMap<>();

        for (TemporalDatabase b : st.consumers) {
            boolean hasSimpleResolution = false;
            List<Resolver> ops = APlanner.currentPlanner.GetSupporters(b, st);
            for (Resolver o : ops) {
                if (o.representsCausalLinkAddition()) {
                    hasSimpleResolution = true;
                    break;
                }
            }
            if (!hasSimpleResolution) {
                VarRef v = b.GetGlobalConsumeValue();
                List<String> atoms = GetAtomNames(st, b.stateVariable, v);
                if (atoms.size() > 1) {
                    continue; //we take only the instantiated atoms
                }
                String name = atoms.get(0);
                if(!RelaxedGroundAtom.Indexed(name)){
                    continue; //static atom - useless
                }
                RelaxedGroundAtom at = new RelaxedGroundAtom(name);
                if (name != null) {
                    goal.set(at.mID);
                    Slice s = new Slice(name, b.getConsumeTimePoint());
                    goalSlice.put(s.variable, s);
                }
            }
        }
        if (goal.isEmpty()) {
            st.h = 0; //no goals
            return;
        }
        //now we make a slice of the initial state we are considering
        if (true || LMCut.commonInit == null) {
            for (TemporalDatabase b : st.tdb.vars) {
                if (b.HasSinglePersistence()) {
                    continue;
                }
                List<String> names = b.GetPossibleSupportAtomNames(st);
                for (String nm : names) {
                    if (RelaxedGroundAtom.Indexed(nm)) {
                        Slice s = new Slice(nm, b.getConsumeTimePoint());
                        Slice ss = goalSlice.get(s.variable);
                        Slice sss = initSlice.get(s.variable);
                        //add only those slices to the init that can be ordered before the corresponding goal slice
                        // do not add slices that must necesseraly occur before some other slice of the same state variable
                        if ((ss == null || st.tempoNet.CanBeBefore(s.when, ss.when))
                                && (sss == null || !st.tempoNet.CanBeBefore(s.when, sss.when))) {
                            initSlice.put(s.variable, s);
                        }
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
        //System.out.println(st.h);
    }

    public static LMCut lmc = null;
    public static AnmlProblem currentProblem = null;
    //public static int cnt = 0;

    public float cost(State st) {
        if (st.g == -1 && st.h == -1) {
            if (lmc == null || st.pb != currentProblem) {
                currentProblem = st.pb;
                lmc = new LMCut(new GroundProblem(st.pb));
            }
            //System.out.print(cnt++);
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
