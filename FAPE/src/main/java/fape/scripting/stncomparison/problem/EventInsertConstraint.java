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
import java.util.Random;
import planstack.anml.model.concrete.TPRef;

/**
 *
 * @author FD
 */
public class EventInsertConstraint extends Event {

    private class Constraint {

        public int min, max, a, b;
    }

    public static int conRangeMin = 10, conRangeMax = 1000, conAddion = 3000;

    private Constraint innerRG( Random rg, int top) {
        Constraint c = new Constraint();
        //only with having at least two timepoints this makes sense
        c.a = rg.nextInt(top);
        c.b = c.a;
        while (c.a == c.b) {
            c.b = rg.nextInt(top);
        }
        if (c.a > c.b) {
            int pm = c.a;
            c.a = c.b;
            c.b = pm;
        }
        c.min = rg.nextInt(conRangeMin);
        c.max = rg.nextInt(conRangeMax) + conAddion;
        return c;
    }

    @Override
    public void Apply(STNManagerOrig o, Random rg) {
        int top = o.getMax();
        if (top > 2) {
            Constraint c = innerRG(rg, top);
            if (o.EdgeConsistent(c.a, c.b, c.min, c.max)) {
                o.EnforceConstraint(c.a, c.b, c.min, c.max);
            } else {
                throw new FAPEException("inconsistent");
            }
        }
    }

    @Override
    public void Apply(STNManager n, Random rg, STNScenario s) {

        int top = n.stn.size()-2;
        if (top > 2) {
            Constraint c = innerRG(rg, top);
            n.EnforceConstraint(new TPRef(c.a+2), new TPRef(c.b+2), c.min, c.max);
        }
    }

}
