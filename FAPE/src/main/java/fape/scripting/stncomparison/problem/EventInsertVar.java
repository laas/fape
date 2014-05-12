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

package fape.scripting.stncomparison.problem;

import fape.core.planning.stn.STNManager;
import fape.scripting.stncomparison.fullstn.STNManagerOrig;
import java.util.Random;
import planstack.anml.model.concrete.TPRef;

/**
 *
 * @author FD
 */
public class EventInsertVar extends Event  {

    @Override
    public void Apply(STNManagerOrig o, Random rg) {
        o.getNewVariable();
    }

    @Override
    public void Apply(STNManager n, Random rg, STNScenario s) {
        n.recordTimePoint(new TPRef(s.varCount+2));
        s.varCount++;
    }

}
