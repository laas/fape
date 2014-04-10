package fape.core.planning.planner;

import fape.core.planning.Planner;
import fape.core.planning.preprocessing.ActionSupporterFinder;
import fape.core.planning.preprocessing.LiftedDTG;
import planstack.anml.parser.ParseResult;

public class BaseDTG extends Planner {

    LiftedDTG dtg = null;

    @Override
    public String shortName() {
        return "base+dtg";
    }

    @Override
    public boolean ForceFact(ParseResult anml) {
        super.ForceFact(anml);
        dtg = new LiftedDTG(pb);
        return true;
    }

    @Override
    public ActionSupporterFinder getActionSupporterFinder() {
        return dtg;
    }
}
