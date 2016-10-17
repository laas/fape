package fr.laas.fape.planning.core.planning.preprocessing;

import fr.laas.fape.anml.model.AnmlProblem;
import fr.laas.fape.anml.model.Type;
import fr.laas.fape.anml.model.abs.AbstractAction;
import fr.laas.fape.anml.model.abs.statements.AbstractAssignment;
import fr.laas.fape.anml.model.abs.statements.AbstractLogStatement;
import fr.laas.fape.anml.model.abs.statements.AbstractTransition;
import fr.laas.fape.planning.core.planning.states.State;
import fr.laas.fape.planning.core.planning.timelines.Timeline;
import fr.laas.fape.planning.util.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class LiftedDTG implements ActionSupporterFinder{

    private final AnmlProblem problem;

    private Map<FluentType, List<SupportingAction>> potentialSupporters = null;

    public LiftedDTG(AnmlProblem pb) {
        this.problem = pb;

        Map<FluentType, Set<SupportingAction>> temporaryPotentialSupporters = new HashMap<>();

        // build the constraint between fluents types
        for(AbstractAction aa : problem.abstractActions()) {
            for(AbstractLogStatement s : aa.jLogStatements()) {
                if(s instanceof AbstractTransition || s instanceof AbstractAssignment) {
                    for(FluentType eff : getEffects(aa, s)) {
                        if(!temporaryPotentialSupporters.containsKey(eff))
                            temporaryPotentialSupporters.put(eff, new HashSet<>());
                        temporaryPotentialSupporters.get(eff).add(new SupportingAction(aa, s.id()));
                    }
                }
            }
        }

        potentialSupporters = new HashMap<>();
        for(FluentType ft : temporaryPotentialSupporters.keySet()) {
            potentialSupporters.put(ft, Utils.asImmutableList(temporaryPotentialSupporters.get(ft)));
        }
    }

    public Collection<SupportingAction> getActionsSupporting(State st, Timeline db) {
        assert db.isConsumer() : "Error: this database doesn't need support: "+db;

        String predicate = db.stateVariable.func().name();
        List<Type> argTypes = Arrays.asList(db.stateVariable.args()).stream()
                .map(arg -> arg.getType())
                .collect(Collectors.toList());
        return this.getActionsSupporting(new FluentType(predicate, argTypes, db.getGlobalConsumeValue().getType()));
    }

    private Collection<SupportingAction> getActionsSupporting(FluentType f) {
        if(!potentialSupporters.containsKey(f))
            potentialSupporters.put(f, Collections.emptyList());
        return potentialSupporters.get(f);
    }

    private Set<FluentType> getEffects(AbstractAction a, AbstractLogStatement s) {
        if(!s.hasEffectAtEnd())
            return Collections.emptySet();

        Set<FluentType> allEffects = new HashSet<>();
        List<Type> argTypes = s.sv().jArgs().stream().map(x -> x.getType()).collect(Collectors.toList());
        Type valType = s.effectValue().getType();

        FluentType fluent = new FluentType(s.sv().func().name(), argTypes, valType);
        allEffects.addAll(derivedSubTypes(fluent));
        allEffects.addAll(derivedSuperTypes(fluent));
        return allEffects;
    }

    public Set<FluentType> getEffects(AbstractAction a) {
        Set<FluentType> allEffects = new HashSet<>();
        for(AbstractLogStatement s : a.jLogStatements()) {
            allEffects.addAll(getEffects(a, s));
        }
        return allEffects;
    }

    private Set<FluentType> getPreconditions(AbstractAction a, AbstractLogStatement s) {
        if(!s.hasConditionAtStart())
            return Collections.emptySet();

        Set<FluentType> allPrecond = new HashSet<>();
        List<Type> argTypes = s.sv().jArgs().stream().map(x -> x.getType()).collect(Collectors.toList());
        Type valType = s.conditionValue().getType();

        FluentType fluent = new FluentType(s.sv().func().name(), argTypes, valType);
        allPrecond.addAll(derivedSubTypes(fluent));
        allPrecond.addAll(derivedSuperTypes(fluent));
        return allPrecond;
    }

    public Set<FluentType> getPreconditions(AbstractAction a) {
        Set<FluentType> allPrecond = new HashSet<>();

        for(AbstractLogStatement s : a.jLogStatements()) {
            allPrecond.addAll(getPreconditions(a, s));
        }

        return allPrecond;
    }

    /**
     * Given a fluent type ft, returns all fluent types where the arg and value
     * types are subclasses of those of ft.
     *
     * This set includes ft itself.
     * @param ft
     * @return
     */
    private Set<FluentType> derivedSubTypes(FluentType ft) {
        Set<FluentType> allFluents = new HashSet<>();
        if(ft == null) {
            return allFluents;
        } else {
            List<List<Type>> argTypesSets = new LinkedList<>();
            for(Type argType : ft.argTypes) {
                argTypesSets.add(new LinkedList<>(argType.jAllSubTypes()));
            }

            for(List<Type> argTypeList : Utils.allCombinations(argTypesSets)) {
                for(Type valueType : ft.valueType.jAllSubTypes()) {
                    allFluents.add(new FluentType(ft.predicateName, argTypeList, valueType));
                }
            }
        }
        return allFluents;
    }

    /**
     * Given a fluent type ft, returns all fluent types where the arg and value
     * types are super types of those of ft.
     *
     * This set includes ft itself.
     * @param ft
     * @return
     */
    private Set<FluentType> derivedSuperTypes(FluentType ft) {
        Set<FluentType> allFluents = new HashSet<>();
        if(ft == null) {
            return allFluents;
        } else {
            List<List<Type>> argTypesSets = new LinkedList<>();
            for(Type argType : ft.argTypes) {
                List<Type> parentsAndSelf = new LinkedList<>(argType.jParents());
                parentsAndSelf.add(argType);
                argTypesSets.add(parentsAndSelf);
            }

            for(List<Type> argTypeList : Utils.allCombinations(argTypesSets)) {
                for(Type valueType : ft.valueType.jParents()) {
                    allFluents.add(new FluentType(ft.predicateName, argTypeList, valueType));
                }
            }
        }
        return allFluents;
    }
}
