package fr.laas.fape.planning.core.planning.grounding;

import fr.laas.fape.anml.model.*;
import fr.laas.fape.anml.model.abs.*;
import fr.laas.fape.anml.model.abs.statements.*;
import fr.laas.fape.anml.model.abs.time.AbsTP;
import fr.laas.fape.anml.model.concrete.InstanceRef;
import fr.laas.fape.anml.model.concrete.VarRef;
import fr.laas.fape.anml.pending.IntExpression;
import fr.laas.fape.anml.pending.LStateVariable;
import fr.laas.fape.planning.core.inference.HReasoner;
import fr.laas.fape.planning.core.inference.Predicate;
import fr.laas.fape.planning.core.inference.Term;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.exceptions.FAPEException;
import fr.laas.fape.planning.exceptions.NotValidGroundAction;
import fr.laas.fape.planning.util.Pair;
import fr.laas.fape.structures.Ident;
import fr.laas.fape.structures.Identifiable;
import lombok.Getter;


import java.util.*;
import java.util.stream.Collectors;

@Ident(GAction.class)
public class GAction implements Identifiable {



    public static abstract class GLogStatement {
        public final GStateVariable sv;
        @Getter public final int minDuration;
        public final AbstractLogStatement original;
        public final Optional<GAction> container;
        public GLogStatement(GStateVariable sv, int minDuration,
                             AbstractLogStatement original,
                             Optional<GAction> container) {
            this.sv = sv;
            this.minDuration = minDuration;
            this.original = original;
            this.container = container;
            assert minDuration >= 0;
        }
        public final GStateVariable getStateVariable() { return sv; }
        abstract public boolean isChange();
        public boolean isTransition() { return this instanceof GTransition; };
        public boolean isPersistence() { return this instanceof GPersistence; };
        public boolean isAssignement() { return this instanceof GAssignment; };
        abstract public InstanceRef endValue();
        abstract public InstanceRef startValue();
        abstract public Fluent getStartFluent();
        abstract public Fluent getEndFluent();
        public AbsTP start() { return original.start(); }
        public AbsTP end() { return original.end(); }
    }
    public static class GTransition extends GLogStatement {
        @Getter private final Fluent startFluent;
        @Getter private final Fluent endFluent;
        public GTransition(Fluent startFluent, Fluent endFluent,
                           int minDuration, AbstractLogStatement original, Optional<GAction> container) {
            super(startFluent.sv, minDuration, original, container);
            assert startFluent.sv.equals(endFluent.sv);
            this.startFluent = startFluent;
            this.endFluent = endFluent;
        }
        @Override public final boolean isChange() { return true; }
        @Override public String toString() { return sv.toString()+"="+ startValue() +"->"+ endValue(); }
        @Override public InstanceRef startValue() { return startFluent.value; }
        @Override public InstanceRef endValue() { return endFluent.value; }
    }
    public static final class GAssignment extends GLogStatement {
        @Getter public final Fluent fluent;
        public GAssignment(Fluent value, int minDuration, AbstractLogStatement original, Optional<GAction> container) {
            super(value.sv, minDuration, original, container);
            this.fluent = value;
        }
        @Override public final boolean isChange() { return true; }
        @Override public String toString() { return sv.toString()+":="+endValue(); }
        @Override public InstanceRef startValue() { throw new FAPEException("Assignments define no start value."); }
        @Override public InstanceRef endValue() { return fluent.value; }
        @Override public Fluent getStartFluent() { throw new FAPEException("Assignments define no start value."); }
        @Override public Fluent getEndFluent() { return fluent; }
    }
    public static final class GPersistence extends GLogStatement {
        public final Fluent fluent;
        public GPersistence(Fluent f, int minDuration, AbstractLogStatement original, Optional<GAction> container) {
            super(f.sv, minDuration, original, container);
            this.fluent = f;
        }
        @Override public final boolean isChange() { return false; }
        @Override public String toString() { return sv.toString()+"=="+endValue(); }
        @Override public InstanceRef startValue() { return fluent.value; }
        @Override public InstanceRef endValue() { return fluent.value; }
        @Override public Fluent getStartFluent() { return fluent; }
        @Override public Fluent getEndFluent() { return fluent; }
    }

    public final List<Fluent> pre = new LinkedList<>();
    public final List<Fluent> add = new LinkedList<>();
    public final int[] preconditions;
    public final int[] additions;
    public final AbstractAction abs;
    public final GTask task;

    public final List<Pair<LStatementRef, GLogStatement>> gStatements = new LinkedList<>();

    public final LVarRef[] variables;
    protected final InstanceRef[] values;

    public final ArrayList<GTask> subTasks;
    public final GroundProblem ggpb;


    public final int id;

    public GroundProblem.Invariant invariantOf(AbstractParameterizedStateVariable sv, GroundProblem gPb) {
        List<InstanceRef> params = new LinkedList<>();
        for(LVarRef v : sv.jArgs())
            params.add(valueOf(v, gPb.liftedPb));
        for(GroundProblem.Invariant inv : gPb.invariants) {
            if(inv.matches(sv.func(), params))
                return inv;
        }
        return null;
    }

    public GLogStatement statementWithRef(LStatementRef ref) {
        for(Pair<LStatementRef, GLogStatement> p : gStatements) {
            if(p.value1.equals(ref))
                return p.value2;
        }
        throw new FAPEException("Unable to find statement with ref: "+ref);
    }

    public int getNumStatements() {
        return gStatements.size();
    }

    public Collection<GLogStatement> getStatements() {
        return gStatements.stream().map(p -> p.value2).collect(Collectors.toList());
    }

    @Override
    public void setID(int i) {
        throw new FAPEException("Can't modify the ID of a GAction");
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public int hashCode() { return id; }

    @Override
    public boolean equals(Object o) {
        if(o instanceof GAction) return o == this;
        else if(o instanceof Integer) return this.id == (Integer) o;
        else return false;
    }

    public GAction(AbstractAction abs, Map<LVarRef, InstanceRef> bindings, GroundProblem gPb, Planner planner) throws NotValidGroundAction {

        this.ggpb = gPb;
        AnmlProblem pb = ggpb.liftedPb;
        this.abs = abs;

        assert bindings.size() == abs.allVars().length : "Some variables seem to be missing.";

        variables = abs.allVars();
        values = new InstanceRef[variables.length];

        for(int i=0 ; i<variables.length ; i++) {
            assert bindings.containsKey(variables[i]);
            values[i] = bindings.get(variables[i]);
        }

        for(AbstractConstraint as : abs.jConstraints()) {
            if (as instanceof AbstractEqualityConstraint) {
                AbstractEqualityConstraint ec = (AbstractEqualityConstraint) as;
                GroundProblem.Invariant inv = invariantOf(ec.sv(), gPb);
                if (inv == null || inv.value != valueOf(ec.variable(), pb)) {
                    throw new NotValidGroundAction("Action not valid1");
                }
            } else if (as instanceof AbstractInequalityConstraint) {
                AbstractInequalityConstraint ec = (AbstractInequalityConstraint) as;
                GroundProblem.Invariant inv = invariantOf(ec.sv(), gPb);
                if (inv == null || inv.value == valueOf(ec.variable(), pb)) {
                    throw new NotValidGroundAction("Action not valid2");
                }
            } else if (as instanceof AbstractVarEqualityConstraint) {
                AbstractVarEqualityConstraint ec = (AbstractVarEqualityConstraint) as;
                if (valueOf(ec.leftVar(), pb) != valueOf(ec.rightVar(), pb))
                    throw new NotValidGroundAction("Action not valid3");
            } else if (as instanceof AbstractVarInequalityConstraint) {
                AbstractVarInequalityConstraint ec = (AbstractVarInequalityConstraint) as;
                if (valueOf(ec.leftVar(), pb) == valueOf(ec.rightVar(), pb))
                    throw new NotValidGroundAction("Action not valid4");
            }
        }
        for(AbstractStatement as : abs.jStatements()) {
            if(as instanceof AbstractTransition) {
                AbstractTransition t = (AbstractTransition) as;
                GLogStatement gls = new GTransition(
                        fluent(t.sv(), t.from(), planner),
                        fluent(t.sv(), t.to(), planner),
                        minDuration(t.start(), t.end()),
                        t, Optional.of(this));
                gStatements.add(new Pair<>(t.id(), gls));
                pre.add(fluent(t.sv(), t.from(), planner));
                if(!fluent(t.sv(), t.from(), planner).equals(fluent(t.sv(), t.to(), planner))) {
                    add.add(fluent(t.sv(), t.to(), planner));
                }

            } else if(as instanceof AbstractPersistence) {
                AbstractPersistence p = (AbstractPersistence) as;
                GLogStatement gls = new GPersistence(
                        fluent(p.sv(), p.value(), planner),
                        minDuration(p.start(), p.end()),
                        p,
                        Optional.of(this));
                gStatements.add(new Pair<>(p.id(), gls));
                pre.add(fluent(p.sv(), p.value(), planner));
            } else if(as instanceof AbstractAssignment) {
                AbstractAssignment a = (AbstractAssignment) as;
                GLogStatement gls = new GAssignment(
                        fluent(a.sv(), a.value(), planner),
                        minDuration(a.start(), a.end()),
                        a,
                        Optional.of(this));
                gStatements.add(new Pair<>(a.id(), gls));
                add.add(fluent(a.sv(), a.value(), planner));
            }
        }

        for(Pair<LStatementRef,GLogStatement> p : gStatements) {
            GStateVariable sv = p.value2.sv;
            if(p.value2.isChange()) {
                AbsTP start = abs.getLogStatement(p.value1).start();
                AbsTP end = abs.getLogStatement(p.value1).end();
                for(Pair<LStatementRef,GLogStatement> p2 : gStatements) {
                    if(p == p2)
                        continue;
                    if(sv != p2.value2.sv)
                        continue;
                    AbsTP start2 = abs.getLogStatement(p2.value1).start();
                    AbsTP end2 = abs.getLogStatement(p2.value1).end();
                    if(abs.stn().strictlyBetween(start2, start, end) ||
                            abs.stn().strictlyBetween(end2, start, end) ||
                            (abs.stn().beforeOrConcurrent(start2, start) && abs.stn().beforeOrConcurrent(end, end2)))
                        throw new NotValidGroundAction("Two conflicting statements on the same state variable.");
                }
            }
        }

        // with temporal actions, a lot of actions can be self suportive
        pre.removeAll(add);

        this.id = planner.preprocessor.nextGActionID++;
        this.subTasks = initSubTasks(gPb.liftedPb, planner);
        this.task = initTask(gPb.liftedPb, planner);
        this.preconditions = new int[pre.size()];
        for(int i=0 ; i<pre.size() ; i++)
            this.preconditions[i] = pre.get(i).getID();
        this.additions = new int[add.size()];
        for(int i=0 ; i<add.size() ; i++)
            this.additions[i] = add.get(i).getID();
    }

    public int minDuration(AbsTP from, AbsTP to) {
        return evaluate(abs.minDelay(from,to));
    }
    public int maxDuration(AbsTP from, AbsTP to) {
        return this.evaluate(abs.maxDelay(from, to));
    }

    public int evaluate(IntExpression e) {
        java.util.function.Function<IntExpression,IntExpression> t = expr -> {
                if(expr instanceof LStateVariable) {
                    List<InstanceRef> params = ((LStateVariable) expr).lsv().jArgs().stream()
                            .map(v -> valueOf(v, ggpb.liftedPb))
                            .collect(Collectors.toList());
                    GroundProblem.IntegerInvariantKey o = new GroundProblem.IntegerInvariantKey(((LStateVariable) expr).lsv().func(), params);
                    if(!ggpb.intInvariants.containsKey(o))
                        throw new NotValidGroundAction("Some of its duration depends on an undefined constant.");
                    int realValue = ggpb.intInvariants.get(o);
                    return IntExpression.lit(realValue);
                } else {
                    return expr;
                }
            };
        IntExpression binded = e.jTrans(t);
        assert binded.isKnown();
        return binded.get();
    }

    @Override
    public String toString() {
        String ret = "("+id+")";
        ret += abs.name()+ "(";
        for(int j=0 ; j<abs.args().size() ; j++) {
            ret += valueOf(abs.args().get(j));
            if(j < abs.args().size()-1)
                ret += ", ";
        }
        ret +=") ";
        for(int i=0 ; i< variables.length ; i++) {
            if(!abs.args().contains(variables[i]))
                ret += variables[i] +":"+ values[i]+" ";
        }
        return ret;
    }

    /** Returns the instance corresponding to this local variable.
     *  This instance must have been declared in the action (ie. not a global variable. */
    public InstanceRef valueOf(LVarRef v) {
        for(int i=0 ; i< variables.length ; i++)
            if(variables[i].equals(v))
                return values[i];
        throw new FAPEException("This local variable was not found: "+v);
    }

    /** Returns the instance corresponding to this local ref.
     *  If this ref was not declared in the action, it will look for global variables. */
    public InstanceRef valueOf(LVarRef v, AnmlProblem pb) {
        // first look in local variables
        for(int i=0 ; i< variables.length ; i++)
            if(variables[i].equals(v))
                return values[i];

        // it does not appear in local variables, it is a global one
        return (InstanceRef) pb.context().getDefinition(v);
    }

    private GStateVariable sv(AbstractParameterizedStateVariable sv, AnmlProblem pb, Planner planner) {
        InstanceRef[] svParams = new InstanceRef[sv.jArgs().size()];
        for(int i=0 ; i<svParams.length ; i++)
            svParams[i] = valueOf(sv.jArgs().get(i), pb);
        return planner.preprocessor.store.getGStateVariable(sv.func(), Arrays.asList(svParams));
    }

    private Fluent fluent(AbstractParameterizedStateVariable sv, LVarRef value, Planner planner) {
        VarRef[] svParams = new VarRef[sv.jArgs().size()];
        for(int i=0 ; i<svParams.length ; i++)
            svParams[i] = valueOf(sv.jArgs().get(i), planner.pb);
        GStateVariable gsv = planner.preprocessor.getStateVariable(sv.func(), svParams);
        return planner.preprocessor.getFluent(gsv, valueOf(value, planner.pb));
    }

    public Map<Fluent, List<Fluent>> concurrentChanges(Planner planner) {
        Map<Fluent, List<Fluent>> m = new HashMap<>();
        for(AbstractFluent af : abs.concurrentChanges().keySet()) {
            Fluent f = fluent(af.sv(), af.value(), planner);
            List<Fluent> conc = abs.concurrentChanges().get(af).stream()
                    .map(abs -> fluent(abs.sv(), abs.value(), planner))
                    .collect(Collectors.toList());
            m.put(f, conc);
        }
        return m;
    }

    public List<GLogStatement> conditionsAt(AbsTP tp, Planner planner) {
        return abs.getConditionsAt(tp).stream()
                .map(p -> statementWithRef(p._2.id()))
                .collect(Collectors.toList());
    }

    public List<GLogStatement> changesStartingAt(AbsTP tp, Planner planner) {
        return abs.getChangesStartingFrom(tp).stream()
                .map(p -> statementWithRef(p.id()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all possible instanciation of the the abstract action in the given decomposition.
     * A possible instanciation is a map from all its local references to instances.
     *
     * If the decoposition ID is not -1, variables declared inside the decomposition will be accounted for as well.
     */
    public static List<Map<LVarRef, InstanceRef>> getPossibleInstantiations(GroundProblem gPb, AbstractAction aa) {
        AnmlProblem pb = gPb.liftedPb;

        // context from which to look for the definition of local variable
        PartialContext context = aa.context();

        // will contain all vars of this action
        List<LVarRef> vars = new LinkedList<>();

        // will contain the domain of each var of this action (same order as in vars)
        List<List<InstanceRef>> possibleValues = new LinkedList<>();

        // get all variables and domains
        for(LVarRef ref : aa.allVars()) {
            vars.add(ref);
            List<InstanceRef> varSet = new LinkedList<>();
            if(ref instanceof EConstantFunction || ! context.hasGlobalVar(ref)) {
                // get the type of the argument and add all possible values to the argument list.
                varSet.addAll(ref.getType().jInstances());
            } else {
                assert (context.getDefinition(ref) instanceof InstanceRef) : "ERRROR: "+context.getDefinition(ref);
                varSet.add((InstanceRef) context.getDefinition(ref));
            }
            possibleValues.add(varSet);
        }

        List<PartialBindings> partialBindingses = new LinkedList<>();

        // look at all static equality constraints to infer partial bindings
        for(AbstractConstraint s : aa.jConstraints()) {
            if(s instanceof AbstractEqualityConstraint) {
                AbstractEqualityConstraint c = (AbstractEqualityConstraint) s;

                // statementVars contains variables that appear as parameters of the function
                // statementConstants contains constants that appear as parameter fo the function
                // for instance the statement `connected(a, b) == true;` would give:
                // statementsVars = {a, b, null} and constantVars == {null, null, true}
                LVarRef[] statementVars = new LVarRef[c.sv().jArgs().size()+1];
                InstanceRef[] statementConstants = new InstanceRef[c.sv().jArgs().size()+1];
                int numVariables = 0;
                for(int i=0 ; i <= c.sv().jArgs().size() ; i++) {
                    LVarRef locVar;
                    if(i < c.sv().jArgs().size()) // get all arguments first
                        locVar = c.sv().jArgs().get(i);
                    else // last one is the variable
                        locVar = c.variable();

                    if(vars.contains(locVar)) {
                        statementVars[i] = locVar;
                        numVariables++;
                    } else {
                        assert pb.instances().containsInstance(locVar.id()) : "Could not find the definition of variable '"+locVar+ "' in action "+aa.name();
                        statementConstants[i] = pb.instance(locVar.id());
                    }
                }
                for(int i=0 ; i<statementVars.length ; i++)
                    assert statementVars[i] == null && statementConstants[i] != null || statementVars[i] != null && statementConstants[i] == null;

                LVarRef[] holeFreeStatementVariables = new LVarRef[numVariables];
                int j = 0;
                for(LVarRef v : statementVars) {
                    if(v != null) {
                        holeFreeStatementVariables[j] = v;
                        j++;
                    }
                }
                assert j == numVariables;

                // new partial bindings reflecting this constraint
                PartialBindings partialBindings = new PartialBindings(vars.toArray(new LVarRef[vars.size()]), holeFreeStatementVariables, possibleValues);
                partialBindingses.add(partialBindings);

                // every invariant matching our state variable proposes possible values
                for(GroundProblem.Invariant inv : gPb.invariants) {
                    if(c.sv().func() == inv.f) {
                        InstanceRef[] binding = new InstanceRef[statementVars.length];
                        for(int i=0 ; i<inv.params.size() ; i++) {
                            binding[i] = inv.params.get(i);
                        }
                        binding[statementVars.length-1] = inv.value;

                        InstanceRef[] variablesOnlyBinding = new InstanceRef[numVariables];
                        int nextVarIndex = 0; // next position in variables only binding
                        boolean valid = true; // is this binding valid wrt constaints
                        for(int i=0 ; i<binding.length ; i++) {
                            if(statementVars[i] != null) {
                                variablesOnlyBinding[nextVarIndex++] = binding[i];
                            } else {
                                assert statementConstants[i] != null;
                                if(!statementConstants[i].equals(binding[i]))
                                    valid = false;
                            }
                        }
                        if(valid)
                            partialBindings.addPartialBinding(variablesOnlyBinding, gPb);
                    }
                }
            }
        }

        if(partialBindingses.isEmpty())
            // empty partial binding, to be used as no other was produced (happens when no static constraints)
            partialBindingses.add(new PartialBindings(vars.toArray(new LVarRef[vars.size()]), new LVarRef[0], possibleValues));

        // treat equalities between variables. those are merge into existing partial bindings
        for(AbstractConstraint s : aa.jConstraints()) {
            if(s instanceof AbstractVarEqualityConstraint) {
                AbstractVarEqualityConstraint c = (AbstractVarEqualityConstraint) s;
                // find a partial binding in which to merge, this is the one with shares the maximum of variables with this equality constraint
                int bestNumOverlappingVariables = -1;
                PartialBindings best = null;
                for(PartialBindings partialBindings : partialBindingses) {
                    int numOverlapping = partialBindings.focusesOn(c.leftVar()) ? 1 : 0;
                    numOverlapping += partialBindings.focusesOn(c.rightVar()) ? 1 : 0;
                    if(numOverlapping > bestNumOverlappingVariables) {
                        bestNumOverlappingVariables = numOverlapping;
                        best = partialBindings;
                    }
                }
                assert best != null;
                if(vars.contains(c.leftVar()) && vars.contains(c.rightVar())) {
                    // merge this equality into the best looking PartialBindings
                    best.addEquality(c.leftVar(), c.rightVar());
                } else if(!vars.contains(c.leftVar()) && vars.contains(c.rightVar())) {
                    InstanceRef value = pb.instance(c.leftVar().id());
                    best.bind(c.rightVar(), value);
                } else if(vars.contains(c.leftVar()) && !vars.contains(c.rightVar())) {
                    InstanceRef value = pb.instance(c.rightVar().id());
                    best.bind(c.leftVar(), value);
                } else {
                    System.out.println("Warning: equality constraint on two constants: "+c);
                    InstanceRef leftVal = pb.instance(c.leftVar().id());
                    InstanceRef rightVal = pb.instance(c.rightVar().id());
                    if(leftVal != rightVal) // two different cosntant required to be equal
                        return Collections.emptyList();
//                    throw new FAPEException("Equality constraint between two constants: "+s+" in action "+aa.name());
                }
            }
        }

        // merge all partial bindings into one
        PartialBindings mergeReceiver = partialBindingses.get(0);
        for(int i=1 ; i<partialBindingses.size() ; i++) {
            mergeReceiver.merge(partialBindingses.get(i));
        }

        // get all possible instantiations from our partial binding
        List<InstanceRef[]> instantiations = mergeReceiver.instantiations();
        List<Map<LVarRef, InstanceRef>> paramsLists = new LinkedList<>();

        for(InstanceRef[] instantiation : instantiations) {
            Map<LVarRef, InstanceRef> params = new HashMap<>();
            assert instantiation.length == vars.size();
            for(int i=0 ; i< vars.size() ; i++) {
                assert instantiation[i] != null : "There is a problem with instantiation.";
                params.put(vars.get(i), instantiation[i]);
            }
            paramsLists.add(params);
        }
        return paramsLists;
    }

    public static List<GAction> groundActions(GroundProblem gPb, AbstractAction aa, Planner planner) {
        // all ground actions corresponding to aa
        List<GAction> actions = new LinkedList<>();

        List<Map<LVarRef, InstanceRef>> paramsLists = getPossibleInstantiations(gPb, aa);
        for(Map<LVarRef, InstanceRef> params : paramsLists) {
            try {
                actions.add(new GAction(aa, params, gPb, planner));
            } catch (NotValidGroundAction e) {}
        }

        for(GAction ga : actions)
            planner.preprocessor.store.record(ga);

        return actions;
    }

    private GTask initTask(AnmlProblem pb, Planner planner) {
        List<InstanceRef> args = new LinkedList<>();
        for(LVarRef v : abs.args()) {
            args.add(valueOf(v, pb));
        }
        return planner.preprocessor.store.getTask(abs.taskName(), args);
    }

    public ArrayList<GTask> getSubtasks() {
        return subTasks;
    }

    public ArrayList<GTask> initSubTasks(AnmlProblem pb, Planner planner) {
        List<AbstractTask> refs = this.abs.jSubTasks();
        ArrayList<GTask> ret = new ArrayList<>();

        for(AbstractTask ref : refs) {
            List<InstanceRef> args = new ArrayList<>();
            for(LVarRef v : ref.jArgs()) {
                args.add(valueOf(v, pb));
            }
            ret.add(planner.preprocessor.store.getTask(ref.name(), args));
        }

        return ret;
    }

    public void addClauses(HReasoner<Term> r) {
        Predicate sup = new Predicate(Predicate.PredicateName.SUPPORTED, this);
        Term[] preTerms = new Term[pre.size()+1];
        preTerms[0] = new Predicate(Predicate.PredicateName.ACCEPTABLE, this);
        for(int i=0 ; i<pre.size() ; i++)
            preTerms[i+1] = pre.get(i);
        // supported(a) :- acceptable(a), precond1, precond2, ...
        r.addHornClause(sup, preTerms);
        for(Fluent f : add)
            // effect_i :- supported(a)
            r.addHornClause(f, sup);

        // feasible(task_a) :- supported(a)
        r.addHornClause(new Predicate(Predicate.PredicateName.FEASIBLE, task), sup);

        // decomposable(a) :- supported(a), feasible(subtask1), feasible(subtask2), ...
        Predicate decomposable = new Predicate(Predicate.PredicateName.DECOMPOSABLE, this);
        Term[] subtasks = new Term[getSubtasks().size()+1];
        subtasks[0] = sup;
        for(int i=1 ; i<subtasks.length ; i++) {
            subtasks[i] = new Predicate(Predicate.PredicateName.FEASIBLE, getSubtasks().get(i-1));
        }
        r.addHornClause(decomposable, subtasks);

        if(!abs.isTaskDependent())
            // not motivated: derivable(a) :- decomposable(a)
            r.addHornClause(new Predicate(Predicate.PredicateName.DERIVABLE, this), decomposable);

        // derivable_task(sub_task_i) :- derivable(a)
        for(GTask subTask : subTasks) {
            r.addHornClause(new Predicate(Predicate.PredicateName.DERIVABLE_TASK, subTask), new Predicate(Predicate.PredicateName.DERIVABLE, this));
        }
        // derivable(a) :- derivable_task(task(a))
        r.addHornClause(new Predicate(Predicate.PredicateName.DERIVABLE, this), new Predicate(Predicate.PredicateName.DERIVABLE_TASK, task), decomposable);

        r.addHornClause(new Predicate(Predicate.PredicateName.POSSIBLE_IN_PLAN, this), new Predicate(Predicate.PredicateName.DERIVABLE, this));
        r.addHornClause(new Predicate(Predicate.PredicateName.POSSIBLE_IN_PLAN, this), new Predicate(Predicate.PredicateName.IN_PLAN, this), new Predicate(Predicate.PredicateName.DECOMPOSABLE, this));
    }
}
