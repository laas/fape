package fape.core.planning.planner;

import fape.core.planning.preprocessing.AbstractionHierarchy;
import planstack.anml.parser.ParseResult;

public class BaseDTGAbsHier extends BaseDTG {

    AbstractionHierarchy hierarchy = null;

    @Override
    public String shortName() {
        return "base+dtg+abs";
    }

    @Override
    public boolean ForceFact(ParseResult anml, boolean propagate) {
        super.ForceFact(anml, propagate);
        this.hierarchy = new AbstractionHierarchy(this.pb);
        return true;
    }
}
