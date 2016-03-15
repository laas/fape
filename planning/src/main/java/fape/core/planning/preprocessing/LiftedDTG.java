package fape.core.planning.preprocessing;

import fape.core.planning.planninggraph.PGUtils;
import fape.core.planning.states.State;
import fape.core.planning.timelines.Timeline;
import fape.exceptions.FAPEException;
import fape.util.Utils;
import planstack.anml.model.AnmlProblem;
import planstack.anml.model.Function;
import planstack.anml.model.LVarRef;
import planstack.anml.model.SymFunction;
import planstack.anml.model.abs.AbstractAction;
import planstack.anml.model.abs.statements.AbstractAssignment;
import planstack.anml.model.abs.statements.AbstractLogStatement;
import planstack.anml.model.abs.statements.AbstractPersistence;
import planstack.anml.model.abs.statements.AbstractTransition;
import planstack.anml.model.concrete.VarRef;
import scala.collection.JavaConversions;

import java.util.*;

public class LiftedDTG implements ActionSupporterFinder{

    final AnmlProblem problem;

    Map<FluentType, List<SupportingAction>> potentialSupporters = null;

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
        List<String> argTypes = new LinkedList<>();
        for(VarRef argVar : db.stateVariable.args()) {
            argTypes.add(st.typeOf(argVar));
        }
        String valueType = st.typeOf(db.getGlobalConsumeValue());
        return this.getActionsSupporting(new FluentType(predicate, argTypes, valueType));
    }

    public Collection<SupportingAction> getActionsSupporting(FluentType f) {
        if(!potentialSupporters.containsKey(f))
            potentialSupporters.put(f, Collections.emptyList());
        return potentialSupporters.get(f);
    }

    public Set<FluentType> getEffects(AbstractAction a, AbstractLogStatement s) {
        Set<FluentType> allEffects = new HashSet<>();
        List<String> argTypes = new LinkedList<>();
        String valType = null;
        if(s instanceof AbstractTransition) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractTransition) s).to());

        } else if(s instanceof AbstractAssignment) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractAssignment) s).value());
        } else {
            // this statement has no effects
            return allEffects;
        }

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

    public Set<FluentType> getPreconditions(AbstractAction a, AbstractLogStatement s) {
        Set<FluentType> allPrecond = new HashSet<>();
        List<String> argTypes = new LinkedList<>();
        String valType = null;
        if(s instanceof AbstractTransition) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractTransition) s).from());

        } else if(s instanceof AbstractPersistence) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            valType = a.context().getType(((AbstractPersistence) s).value());
        } else if(s instanceof AbstractAssignment) {
            for(LVarRef arg : s.sv().jArgs()) {
                argTypes.add(a.context().getType(arg));
            }
            // the value before an assignment can be anything with the type of the state variable.
            valType = s.sv().func().valueType();
        } else {
            // this statement has no effects
            return allPrecond;
        }

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
    public Set<FluentType> derivedSubTypes(FluentType ft) {
        Set<FluentType> allFluents = new HashSet<>();
        if(ft == null) {
            return allFluents;
        } else {
            List<List<String>> argTypesSets = new LinkedList<>();
            for(String argType : ft.argTypes) {
                argTypesSets.add(new LinkedList<>(problem.instances().subTypes(argType)));
            }

            for(List<String> argTypeList : PGUtils.allCombinations(argTypesSets)) {
                for(String valueType : problem.instances().subTypes(ft.valueType)) {
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
    public Set<FluentType> derivedSuperTypes(FluentType ft) {
        Set<FluentType> allFluents = new HashSet<>();
        if(ft == null) {
            return allFluents;
        } else {
            List<List<String>> argTypesSets = new LinkedList<>();
            for(String argType : ft.argTypes) {
                List<String> parentsAndSelf = new LinkedList<>(problem.instances().parents(argType));
                parentsAndSelf.add(argType);
                argTypesSets.add(parentsAndSelf);
            }

            for(List<String> argTypeList : PGUtils.allCombinations(argTypesSets)) {
                for(String valueType : problem.instances().parents(ft.valueType)) {
                    allFluents.add(new FluentType(ft.predicateName, argTypeList, valueType));
                }
            }
        }
        return allFluents;
    }
}
