package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import lombok.Value;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.time.AbsTP;

import java.util.List;
import java.util.stream.Collectors;

@Value public class RAct {

    public final GAction act;
    public final AbsTP tp;
    public final List<TempFluent> conditions;
    public final List<TempFluent> effects;

    @Override public String toString() {
        String s = act+"--"+tp.toString()+"\n";
        s += "  conditions:\n";
        for(TempFluent tf : conditions) s += "    "+tf+"\n";
        s += "  effects:\n";
        for(TempFluent tf : effects) s += "    "+tf+"\n";
        return s;
    }


    public static RAct from(DeleteFreeActionsFactory.RActTemplate template, GAction base, GroundProblem pb) {
        assert template.abs == base.abs;

        List<TempFluent> conditions = template.conditions.stream().map(c -> TempFluent.from(c, base, pb)).collect(Collectors.toList());
        List<TempFluent> effects = template.effects.stream().map(c -> TempFluent.from(c, base, pb)).collect(Collectors.toList());

        return new RAct(base, template.tp, conditions, effects);
    }
}
