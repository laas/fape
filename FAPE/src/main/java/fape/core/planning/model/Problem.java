//package fape.core.planning.model;
//
//import fape.core.execution.model.ActionRef;
//import fape.core.execution.model.Instance;
//import fape.core.execution.model.statements.Statement;
//import fape.core.planning.states.State;
//import fape.core.planning.Planner;
//import fape.core.transitions.TransitionIO2Planning;
//import fape.exceptions.FAPEException;
//import fape.util.TinyLogger;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//
//
///**
// * Encodes a planning problem: types, actions and statements.
// * It is responsible for domain-specific preprocessing (DTG, abstractions hierarchies ...).
// *
// * The problem can be updated (typically with new ANML blocks). Those are encoded as elementary revisions
// * to be applied to a state.
// *
// * TODO: support updates from PRS (action executed ...) to be able to create a new state from scratch (when replanning)
// */
////class Problem {
////
////    public class ProblemRevision {
////        public ActionRef addAction = null;
////        public Statement statement = null;
////        public Instance object = null;
////        public ProblemRevision(ActionRef a) {
////            this.addAction = a;
////        }
////        public ProblemRevision(Statement s) {
////            this.statement = s;
////        }
////        public ProblemRevision(Instance newObject) {
////            this.object = newObject;
////        }
////        public boolean isActionAddition() {
////            return addAction != null;
////        }
////        public boolean isStatementAddition() {
////            return statement != null;
////        }
////        public boolean isObjectAddition() {
////            return object != null;
////        }
////    }
////
////    /**
////     * a list of types keyed by its name
////     */
////    public TypeManager types = new TypeManager();
////
////    /**
////     * Contains all ground states variables in the system.
////     */
////    public HashMap<String, StateVariable> vars = new HashMap<>();
////
////    /**
////     *
////     */
////    public HashMap<String, AbstractAction> actions = new HashMap<>();
////
////    public AbstractionHierarchy hierarchy = null;
////
////
////    /**
////     * This contains all revision that have to be applied to a search state that has to
////     * address this problem.
////     *
////     * It is of the form
////     * NumRevision   //  List of updates
////     * 0             //  [AddStatement, addAction, ]
////     * 1             //  [AddStatement]
////     * ....
////     */
////    public List<List<ProblemRevision>> revisions = new LinkedList<>();
////
////    public int currentRevision = -1;
////
////
////    /**
////     * Add the new types/instances/actions to Problem and creates a new revision
////     * containing all updates to be made to a state
////     * @param pl
////     *
////    public void ForceFact(ANMLBlock pl) {
////        List<ProblemRevision> problemUpdates = new LinkedList<>();
////        //read everything that is contained in the ANML block
////        if (Planner.logging) {
////            TinyLogger.LogInfo("Forcing new fact into best state.");
////        }
////
////        // this a generic predecesor of all types
////        if (currentRevision < 0) {
////            types.addType("object", "");
////            types.addType("boolean", "");
////
////            // add true and false to the instances to be processed
////            pl.instances.add(new Instance("true", "boolean"));
////            pl.instances.add(new Instance("false", "boolean"));
////        }
////
////        //convert types
////        for (fape.core.execution.model.types.Type t : pl.types) {
////            types.addType(t.name, t.parent);
////            for(Instance content : t.instances) {
////                types.addContent(t.name, content);
////            }
////        }
////
////        //convert instances and create state variables from them
////        for (Instance i : pl.instances) {
////            types.addInstance(i);
////
////            List<StateVariable> l = TransitionIO2Planning.decomposeInstance("", i.name, i.type, types, i.type, true);
////            for (StateVariable v : l) {
////                vars.put(v.name, v);
////            }
////
////            // Every instance is also a variable with a unique value in their domain.
////            ObjectVariableValues binding = new ObjectVariableValues(i.name, i.type);
////            problemUpdates.add(new ProblemRevision(i));
////        }
////
////        //process statements
////        for (Statement s : pl.statements) {
////            if (!vars.containsKey(s.GetVariableName())) {
////                throw new FAPEException("Unknown state variable: " + s.GetVariableName());
////            }
////            problemUpdates.add(new ProblemRevision(s));
////        }
////
////        //process actions
////        for (fape.core.execution.model.Action a : pl.actions) {
////            if (actions.containsKey(a.name)) {
////                throw new FAPEException("Overriding action abstraction: " + a.name);
////            }
////            AbstractAction act = TransitionIO2Planning.TransformAction(a, this);
////            actions.put(act.name, act);
////        }
////
////        //process seeds
////        for (ActionRef ref : pl.actionsForTaskNetwork) {
////            problemUpdates.add(new ProblemRevision(ref));
////
////        } //end of seed processing
////
////        if (currentRevision < 0) {
////            //create domain transition graphs
////            for (Type t : types.getTypes()) {
////                if (Character.isUpperCase(t.name.charAt(0)) || t.name.equals("boolean")) {//this is an enum type
////                    ADTG dtg = new ADTG(this, t, actions.values());
////
////                    //dtg.op_all_paths(); TODO clean up dtg
////                    dtgs.put(t.name, dtg);
////                }
////            }
////
////            //create the abstraction hierarchy
////            hierarchy = new AbstractionHierarchy(this);
////        }
////
////        revisions.add(problemUpdates);
////        currentRevision++;
////    }*/
////}
