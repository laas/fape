/*
 * Author:  Filip Dvoøák <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvoøák <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.scripting.stncomparison.problem;

import fape.core.planning.stn.STNManager;
import fape.exceptions.FAPEException;
import fape.scripting.stncomparison.fullstn.STNManagerOrig;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author FD
 */
public class STNScenario {

    public static int cnt = 0;
    public int mID = cnt++;
    public int node_count, con_count, check_count;
    public List<Event> eventsToApply = new LinkedList<>();
    int varCount;

    public STNScenario(int max_n, int constraintCount, int consistencyCheckCount) {
        node_count = max_n;
        con_count = constraintCount;
        check_count = consistencyCheckCount;
    }

    @Override
    public String toString() {
        return "Scenario-" + mID + "-" + node_count + "-" + con_count + "-" + check_count;
    }

    public void Apply(STNManagerOrig o, Random rg) {
        int ct = 0;
        for (Event e : eventsToApply) {

            try {
                e.Apply(o, rg);
            } catch (FAPEException ee) {
                System.out.println("Inconsistance reached at step " + ct);
                break;
            }
            ct++;
        }
    }

    public void Apply(STNManager n, Random rg) {
        int ct = 0;
        varCount = 0;
        for (Event e : eventsToApply) {
            try {
                e.Apply(n, rg, this);
            } catch (FAPEException ee) {
                System.out.println("Inconsistance reached at step " + ct);
                break;
            }
            ct++;
        }
    }

}
