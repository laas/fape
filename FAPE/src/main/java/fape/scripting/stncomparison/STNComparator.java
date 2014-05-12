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
package fape.scripting.stncomparison;

import fape.core.planning.stn.STNManager;
import fape.exceptions.FAPEException;
import fape.scripting.stncomparison.fullstn.STNManagerOrig;
import fape.scripting.stncomparison.problem.Event;
import fape.scripting.stncomparison.problem.EventCheckConsistency;
import fape.scripting.stncomparison.problem.EventInsertConstraint;
import fape.scripting.stncomparison.problem.EventInsertVar;
import fape.scripting.stncomparison.problem.STNScenario;
import fape.util.FileHandling;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import planstack.anml.model.concrete.TPRef;
import planstack.constraints.stn.STN;
import planstack.constraints.stn.STNIncBellmanFord;

/**
 *
 * @author FD
 */
public class STNComparator {

    STN stn = new STNIncBellmanFord();

    public static void main(String[] args) {
        HashMap<STNScenario, Result> res = new HashMap<>();
        StringBuilder out = new StringBuilder();
        //generating scenarios
        List<STNScenario> problems = new LinkedList<>();
        Random rg = new Random();
        for (int density = 1; density < 8; density += 1) {
            for (int max_n = 50; max_n < 300; max_n += 50) {
                int constraintCount = density * max_n * max_n / 10;
                int scenarioCount = 100;
                int consistencyCheckCount = max_n * max_n;
                for (int none = 0; none < scenarioCount; none++) {
                    STNScenario s = new STNScenario(max_n, constraintCount, consistencyCheckCount);
                    for (int i = 0; i < max_n; i++) {
                        s.eventsToApply.add(new EventInsertVar());
                    }
                    for (int i = 0; i < constraintCount; i++) {
                        s.eventsToApply.add(new EventInsertConstraint());
                    }
                    for (int i = 0; i < consistencyCheckCount; i++) {
                        s.eventsToApply.add(new EventCheckConsistency());
                    }
                    Collections.shuffle(s.eventsToApply, rg);
                    //problems.add(s);
                    
                    long full, copyfull, bell, copybell;
                    {
                        long start = System.currentTimeMillis();
                        STNManagerOrig o = new STNManagerOrig();
                        o.Init(s.node_count + 1);
                        s.Apply(o, rg);
                        long diff = System.currentTimeMillis() - start;
                        System.out.println("fullstn:" + s.toString() + " time: " + diff + "ms.");
                        //res.get(s).fullTime = diff;
                        //res.get(s).s = s;
                        full = diff;
                        start = System.currentTimeMillis();
                        STNManagerOrig mn = o.DeepCopy();
                        diff = System.currentTimeMillis() - start;
                        copyfull = diff;
                    }

                    {
                        long start = System.currentTimeMillis();
                        STNManager n = new STNManager();
                        s.Apply(n, rg);
                        long diff = System.currentTimeMillis() - start;
                        System.out.println("bellman:" + s.toString() + " time: " + diff + "ms.");
                        //res.get(s).bellTime = diff;
                        bell = diff;
                        start = System.currentTimeMillis();
                        STNManager mn = n.DeepCopy();
                        diff = System.currentTimeMillis() - start;
                        copybell = diff;
                    }
                    
                    out.append(s.toString()).append(",").append(full).append(",").append(copyfull).append(",").append(bell).append(",").append(copybell).append("\n");
                }
            }
        }

        /*
         for (STNScenario s : problems) {
         res.put(s, new Result());
         }*/
        //test on full
        /*for (STNScenario s : problems) {
         long start = System.currentTimeMillis();
         STNManagerOrig o = new STNManagerOrig();
         o.Init(s.node_count + 1);
         s.Apply(o, rg);
         long diff = System.currentTimeMillis() - start;
         System.out.println("fullstn:" + s.toString() + " time: " + diff + "ms.");
         res.get(s).fullTime = diff;
         res.get(s).s = s;
         }*/
        //bellman
        /*for (STNScenario s : problems) {
         long start = System.currentTimeMillis();
         STNManager n = new STNManager();
         s.Apply(n, rg);
         long diff = System.currentTimeMillis() - start;
         System.out.println("bellman:" + s.toString() + " time: " + diff + "ms.");
         res.get(s).bellTime = diff;
         }*/
        //StringBuilder out = new StringBuilder();
        /*for (Result r : res.values()) {
            out.append(r.s.toString()).append(",").append(r.fullTime).append(",").append(r.bellTime).append("\n");
        }*/
        FileHandling.writeFileOutput("results.txt", out.toString());
        int xx = 0;
        //test on bellman

    }

}
