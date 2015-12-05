package fape.core.planning.heuristics.temporal;

import fape.core.planning.grounding.GAction;
import fape.core.planning.grounding.GroundProblem;
import fape.core.planning.planner.APlanner;
import fr.laas.fape.structures.AbsIdentifiable;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.ValueConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.time.AbsTP;

import java.util.List;
import java.util.stream.Collectors;

@Ident(DepGraph.Node.class) @Getter
public class RAct extends DepGraph.ActionNode {

    public final GAction act;
    public final AbsTP tp;
    public final List<TempFluent> conditions;
    public final List<TempFluent> effects;

    @Deprecated @ValueConstructor
    public RAct(GAction act, AbsTP tp, List<TempFluent> conditions, List<TempFluent> effects) {
        this.act = act;
        this.tp = tp;
        this.conditions = conditions;
        this.effects = effects;
    }

    @Override public String toString() {
        String s = act+"--"+tp.toString();
//        s += "  conditions:\n";
//        for(TempFluent tf : conditions) s += "    "+tf+"\n";
//        s += "  effects:\n";
//        for(TempFluent tf : effects) s += "    "+tf+"\n";
        return s;
    }


    public static RAct from(DeleteFreeActionsFactory.RActTemplate template, GAction base, APlanner pl) {
        assert template.abs == base.abs;

        List<TempFluent> conditions = template.conditions.stream()
                .map(c -> TempFluent.from(c, base, pl.preprocessor.getGroundProblem(), pl.preprocessor.store))
                .collect(Collectors.toList());
        List<TempFluent> effects = template.effects.stream()
                .map(c -> TempFluent.from(c, base, pl.preprocessor.getGroundProblem(), pl.preprocessor.store))
                .collect(Collectors.toList());

        return pl.preprocessor.store.getRAct(base, template.tp, conditions, effects);
    }
}
