% FAPE: User Manual
% Arthur Bit-Monnot
%

<!--
Note: you can use Pandoc to generate this document:
# pandoc --standalone --listing -N -o Manual.pdf Manual.md

Otherwise it should be readable as plain text as well.
-->

<!-- This is simply to put the ANML blocks into boxes when generating the PDF -->
\lstset{language=c++}
\lstset{basicstyle=\ttfamily\footnotesize,breaklines=true}
\lstset{morekeywords={action,start,end,all,contains,decomposition,
constant,instance,function,variable}}
\lstset{frame=single}
\lstset{keywordstyle=\bfseries}


This document will give a brief introduction to FAPE.
Its objective is merely to give pointers and some keys on what is currently supported in FAPE and give a rough idea of how things work internally.

Until a more detailed and formal description of the planner is written, we hope it will allow you to start using FAPE without to much pain.
We are aware that this document stays very high level and that it won't answer all question that might arise when using (or contributing to) FAPE. Please ask any question you have, I will be glad to answer it.



# ANML

The objective of this section to describe which subset of ANML is supported in FAPE, assuming the reader already knows the language.
If you are not familiar with ANML, you should first have a look at the ANML manual.

Several examples of valid ANML domains are given in the `domains/` directory.

## Types

    // Defines a type with no parent and no attributes
    type Location;
    
    // Defines a type NavLocation that is a subtype of Location
    // only single inheritance is supported
    type NavLocation < Location;
    
    // Defines a type Robot that is a subclass of Location
    // the variable location defines a function "NavLocation Robot.location(Robot ?)"
    // See Functions for more details.
    type Robot < Location with {
      variable NavLocation location;
    };
    
    // Defines a type Item and a function "Location Item.location(Item)"
    type Item with {
      variable Location location;
    };



## Functions

    function boolean connected(Location a, Location b);


The above ANML block defines a function taking two parameters of type
`Location` and whose return value is of type `boolean`. Functions have an
implicit time parameter (i.e. the value of `connected(Kitchen, Entrance)`
might not be the same during the whole plan).

Several other syntaxes can be used to define functions:

    // a variable is a function with no parameters
    variable Room houseCenter; // is the same as
    function Room houseCenter();

    // a predicate is a function whose value type is boolean
    predicate connected(Location a, Location b); // is the same as
    function boolean connected(Locatin a, Location b);

ANML further allows to define functions whose value is constant over time. We
could rewrite the `connected` function above to make sure its value doesn't
change over time: 

    // a constant is a function whose value doesn't change over time
    constant boolean connected(Location a, Location b);


A facility is provided to define functions in types. The two following ANML
blocks are equivalent:

    type Robot with {
        function boolean canGo(Location l);
    };

    type Robot;
    function boolean Robot.canGo(Robot r, Location l);


Given a `r` instance of `Robot` (or any of its subclasses), writing
`r.canGo(l)` is equivalent to writing `Robot.canGo(r, l)`.



## Logical Statements

A logical statement describes changes and conditions on state variables. It is
associated with a start and en time point.
They refer to state variables: ANML functions with parameters.

 - Persistence: `connected(Kitchen, Entrance) == true;` requires the state
   variable `connected(Kitchen, Entrance)` to be true between the start and
   end time points of the statement.
 - Assignment: `connected(Kitchen, Entrance) := true;` specifies that the
   state variable `connected(Kitchen, Entrance)` will have the value true at
   the end of the statement and is undefined between start and end.
 - Transition: `connected(Kitchen, Entrance) == false :-> true` requires the
   state variable to have the value false at the start of the statement and
   specifies that it will have value `false` at the end of the statement.
   

## Temporal annotations

Temporal annotations are temporal constraints on the timepoints of
statements. Given a statement `s`, where `start(s)` and `end(s)` represent its
start and end time-points.

    [all] s; or [start,end] s;
        => start(s) == start && end(s) == end
    [start+10, end-5] s;
        => start(s) == start +10 && end(s) == end-5
    [2, 10] s;
        => start(s) == 2 && end(s) == 10;
    // or any combination of the above annotations

In the preceding text, `start` and `end` refer to the start and end time
points of the interval containing the annotated statement (such as an action
or a problem).

It is possible to give the same annotation to several statements:

    [start, end] {
      connected(a, b) == true;
      r.canGo(a) := false;
    };
    // is equivalent to
    [start, end] connected(a, b) == true;
    [start, end] r.canGo(a) := false;


A temporal annotation with the `contains` keyword means that the given
statement must be included in the interval:

    [start, end] contains s;
      // start(s) >= start && end(s) <= end

**WARNING**: in its current implementation, the `contains` keyword differs from the mainline ANML definition since it does *not* require the condition to be false before and after the interval. In fact, the above statement is equivalent to `[start,end] contains [0] s;` (but this notation is not supported in FAPE)


## Actions

Action are operators having typed parameters and that might contain any number
of statements. In the following example, the transition statement's start and
end timepoints are equals to those of the action.

    action Move(Robot r, Location a, Location b) {
      [start, end] {
        r.location == a :-> b;
      };
    };

### Duration

Actions can be given a duration either fixed or parameterized with an invariant function of type integer.

    constant integer travel_time(Loc a, Loc b);
    travel_time(A, B) := 10;
    travel_time(A, C) := 15;

    action Move2(Robot r, Loc a, Loc b) {
      duration := travel_time(a, b);
      ...
    };

The duration can also be left uncertain in a given interval using the keyword `:in`.

    action Move3(Robot, Loc a, Loc b) {
      duration :in [10, 15];
      ...
    };
    
    constant integer min_travel_time(Loc a, Loc b);
    min_travel_time(A, B) := 10;
    min_travel_time(A, C) := 15;
    constant integer max_travel_time(Loc a, Loc b);
    max_travel_time(A, B) := 13;
    max_travel_time(A, C) := 18;

    action Move4(Robot r, Loc a, Loc b) {
      duration :in [min_travel_time(a,b), max_travel_time(a,b)];
    };

If this notation is used, a contingent constraint will be considered between the start and end time points of the action.
It is handled through an STNU framework.

### Decompositions

Actions can be associated with a set of decomposition, if it has one or more decomposition, we call it a non-primitive action.
In a valid plan, every action must be associated with exactly one of its decomposition.
The choice of this decomposition is branching point in the search.

There is no restriction to what can be written in decomposition. Hence it can be used to represent:

- methods in an HTN fashion
- conditional effects
- a controllable choice over the effects of an action. We discourage this practice, since it will not be apparent in the plan.

The following example shows subtasks in decompositions. Those are described in the next sub-section.


        action Pick(Robot r, Item i) {
          :decomposition{
            [all] PickWithLeftGripper(r, i);
          };
          :decomposition{
            [all] PickWithRightGripper(r, i);
          };
        };
        
        action Go(Vehicle v, Loc from, Loc to) {
          [start] location(v) == from;
          :decomposition{
             isCar(v) == true;
             [all] GoByRoad(v, from, to);
          };
          :decomposition{
            isPlane(v) == true;
            [all] Fly(v, from, to);
          };
        };


## Tasks

The last example showed a usage of actions as condition appearing in the decomposition of another one.
This condition, called task, is satisfied if there is an action with the same name and parameters satisfying all temporal constraints on the action condition time points.

    [10,90] Go(PR2, Kitchen, Bedroom);

The above condition will be satisfied iff there is an action `Go` with the parameters `(PR2, Kitchen, Bedroom)` that starts exactly at 10 and ends exactly at 90.

**WARNING:** this differs from the mainline ANML definition because the considered time-points for temporal constraints are those of the action itself.

    // this will be satisfied if there is an Go with this
    // parameters that starts and end within the
    // interval [10,90]
    [10, 90] contains Go(PR2, Kitchen, Bedroom;



    // this action must have exactly the same duration as the
    // one of the ConcreteGo action on which it is conditioned 
    action AbstractGo(Loc a, Loc b) {
      [all] ContreteGo(a, b);
    };

    action ConcreteGo(Loc a, Loc b) {
      duration :in [min_travel_time(a,b), max_travel_time(a,b)];
    };

## Temporal Constraints

Temporal constraints can be specified between intervals (i.e. any ANML object
with start and end time-points such as actions or statements).
The interval must be given a local ID:


    [all] contains {
        idA : I.location == A;
        idB : I.location == B;
    };
    
    // specifies that the second statement must start at least 10 times units
    // after the end of the first one
    end(idA) +10 < start(idB);
    
    // specifies that the second statement must end exactly 60 time units before
    // the end of the containing interval (i.e. the action if the statement is defined
    // in an action or the plan if the statement is defined in the problem).
    end(idB) = end -60;


It is also possible to put temporal constraints between actions conditions. The `ordered` and `unordered` keywords are not supported but can be replaced by temporal constraints as in the following example.

    action PickAndPlace(Robot r, Item i) {
      pickID : Pick(r, i);
      placeID : Place(r, i);
      end(pickID) < start(placeID);
    };

## Binding constraints

Constraints can be expressed between variables and invariant functions.

    constant Country country_of(City c);
    instance Contry France, US, Germany;

    country_of(Paris) == country_of(Toulouse);
    country_of(Chicago) != country_of(Paris);

    // creates a variable "a_country" and constrains it
    // to be different to Paris' country
    constant Country a_country;
    country_of(Paris) != a_country;

## Resources

Resources are not supported yet. An initial implementation is currently in the source repository but is not stable enough for daily usage.



# FAPE: Planner

The FAPE planner is a temporal planner reasoning in plan space. As such, it mainly reasons with flaws/resolvers to reach a solution plan.
As long as there is no task conditions in the domain, FAPE will act as a plan space planner and will try to solve the current open goals and threats in its plan.

It uses a lifted representation and timelines as a time oriented view of the evolution of state variables.

Temporal constraints are managed in an STN extended for the integration of contingent constraints (e.g. the uncertain duration of an action). This STNU framework can be used, while planning different types of consistency: (i) STN consistency (ii) pseudo-controllability (iii) dynamic-controllability.

A binding constraint manager is used to enforce the equality and difference constraints between variables (e.g. parameters of actions).

To put things short: without task conditions, FAPE is a lifted temporal planner, searching in plan-space.


## Hierarchies

In addition to generative planning problems, ANML allows the
definition of hierachical problems.  THis is done through the
definition of of task that should be fulfilled. Such task can appear

 - in the problem statement. In which case they are goal tasks
   (usually refered as the initial task network in the HTN literature)
 - in actions. In which case they are subtasks of this actions.

Any task appearing in the plan (either as part of the problem
definition or inserted with an action) must be refined. We say that a
task `[t1,t2] name(a1...an)` is refined if there is an action
`name(a1,...an)` that starts at `t1` and ends at `t2`.

The open goal and threats flaws from plan-space planning are completed
by an *unrefined task* flaw associated to each task that has not been
refined yet.

A (very) simplified view of FAPE's search procedure is:

 1) refine all tasks. Starting from the initial task network insert
 all actions necessary for all tasks to be refined. If any subtasks
 are inserted during this process, recursively refine those as well.
 2) Make sure the plan is consistent by handling all open goals and
 threats. This implies enforcing causal constraints (all actions'
 conditions must be supported) and temporal consistency (their should
 not be two concurrent and conflicting activities). Enforcing causal
 constraints generally requires the introduction of new actions in the
 partial plan.


Actions might be inserted as standalone resolvers (e.g. to solve an
open goal flaw).  This differs from the HTN way where any action in
the plan is derived from the initial task network.  To mimic this
specificity, ANML provides the ANML keyword: a `motivated` action
needs to refine a task (i.e. it must be part of some hierarchy).  In
practice, it means that only non-motivated actions can be inserted
outside of the action hierarchy.  Making all actions motivated and
giving a root action will result in a HTN-like search, expending a
search tree from the initial task network only.


Unless really sure of what you do, you should avoid having both
motivated actions and goals expressed as statements over state
variable.  The intended way of using FAPE in this setting is:

 - with one or more goal tasks, forming the initial task network
 - motivated actions should be derivable from those goal tasks and will be the skeleton of the plan.
 - non-motivated action are used to handle corner-cases that were not described in the task network.

The following example shows a very simple hierarchical domain where
the skeleton of the plan will derive from Transport. Note that Pick
and Drop are motivated and hence can not appear outside of Transport.
However the Move action will be freely inserted in the plan to tackle
open-goals flaws on the location of the robot.

A typical planning process for this problem would be the following:

1) Refine the `Transport` goal task by inserting a `Transport` action.
2) Refine the `Pick` and `Drop` subtasks of the Transport action by
inserting the actions `Pick(PR2,coffee_cup,Kitchen)` and `Drop(PR2,coffee_cup,Bedroom)`
3) Insert a `Move(PR2, Bedroom, Kitchen)` action to support the condition of `Pick` that PR2 is in the Kitchen.
4) Insert a `Move(PR2,Kitchen,Bedroom)` action to support the condition of `Drop` that PR2 is in the Bedromm.


    type Location;
    type NavLocation < Location;
    type Robot < Location with {
      function NavLocation at();
    };
    type Item with {
      function Location at();
    };

    action Transport(Robot r, Item i, NavLocation a, NavLocation b) {
      motivated;
      [start,t1] Pick(r, i, a); // first subtask
      [t2,end] Drop(r, i, b);   // second subtask
      t1 < t2+0;  // drop comes after pick
    };

    action Drop(Robot r, Item i, NavLocation l) {
      motivated;
      duration := 5;
      [all] r.at == l;
      [all] i.at == r :-> l;
    };

    action Pick(Robot r, Item i, NavLocation l) {
      motivated;
      duration := 5;
      [all] r.at == l;
      [all] i.at == l :-> r;
    };

    action Move(Robot r, NavLocation from, NavLocation to) {
      duration := 5;
      [all] r.at == from :-> to;
    };

    // instances and initial value ....
    instance Robot PR2;
    instance Item coffee_cup;
    instance NavLocation Kitchen, Bedroom;
    [start] PR2.at := Bedroom;
    [start] coffee_cup.at := Kitchen;
    
    // goal task
    Transport(PR2, coffee_cup, Kitchen, Bedroom);

Replacing the `Transport` goal by `[end] coffee_up.at == Bedroom` would fail because
the only resolver would be to insert a Drop action but since Drop is motivated it can not be inserted in the plan (it has to be derived from an action already present in the plan).


## Goals

Any condition appearing in the domain is considered as a goal.
Indeed, when added to the partial it will result in a flaw that must be solved to enforce
plan consistency.

    // example of goals

    // at the end of the plan, the PR2 must be in the kitchen
    [end] PR2.at == Kitchen;

    // for at least one time unit between times 30 and 50,
    // the coffee cup must be on the table.
    [30,50] contains location(coffee_cup) == dining_table;


Similarly, subtasks in the domain definition will result in the
addition of unrefined tasks.  Those unrefined tasks will in turn
trigger the insertion of additional actions.

    // example of objective tasks
    
    // the PR2 must clean the table
    CleanTable(PR2);

    // the PR2 must give the cup before time 100
    [start, 100] contains Give(PR2, Arthur, cup);

    // any robot must transport the coffee cup from the kitchen to the dining room
    constant Robot r;
    Transport(r, coffee_cup, kitchen, dining_room);



## Search: algorithm and strategies

FAPE uses the PSP (Plan Space Planning) algorithm for search:

    queue <- { initial plan }

    while true:
      if queue is empty
        return failure

      p <- pop a partial plan in queue

      flaws <- getFlaws(p)
      
      if flaws is empty
        return p // solution plan

      select any flaw f in flaws
      for resolver in GetResolvers(f, p)
        child <- Apply(resolver, p)
        if child is consistent
          queue <- queue U { child }

    
The two decisions to be made in this algorithm are:

 - which flaw to solve in the current partial plan
 - which partial plan (or which resolver) to consider for the next iteration.

Those are respectively call *flaw selection strategy* and *plan selection strategy*.
The different options for those strategies are describe in the command-line help.

## Temporal uncertainty handling

Temporal planning under uncertainty is of course of interest to us since we do 
not have the exact duration of actions at planning time.
Some of our contingent time-points are observable (e.g. end of an action) which makes 
enforcing dynamic controllability the natural thing to do.
Of course some events might not be observable.

Support for temporal uncertainty is still quite limited. We only support 
contingent constraints between the start and end of an action (assuming that 
the end of the action is observable).
Contingency over non-observable events is naturally supported in ANML with the 
change-over-an-interval notation (this can be seen as a locally strongly 
controllable STNU).

    // an action with uncertain duration
    action Move(Loc a, Loc b) {
      duration :in [15,20];
      ...
    };

What we currently support:
 - uncertain duration of actions (that's the only place where explicit 
contingent constraint are allowed)
 - enforcing pseudo controllability during search (there is a implementation 
for enforcing DC during search but it is not working yet)
 - checking that the plan is dynamically controllable (and keep searching 
otherwise)
 - dispatching the plan (using the dispatching algorithm for dynamically 
controllable STNU)

This guarantees that the plan is executable if:
 - we observe the observable contingent events (verified since we are currently 
limiting ourselves to the end of actions)
 - those happen within the predefined bounds

Things you can try in the main class `fape.Planning`:

 - the option `--stnu` gives you control over which type of controllability you 
enforce during search (dynamic is still experimental)
 - the option `--dispatchable` will check that the plan is dynamically 
controllable and make it dispatchable.

In the main class `fape.FAPE` with option `--sim`, you can try to dispatch a plan 
and simulate random durations and failure (which will trigger plan 
repair/replanning). This environment is still extremely limited, the one we 
use at LAAS relies on OpenPRS and is still under research/development.

# Final note

FAPE is still in a beta state. There might be some bugs left so, if you run into something weird, ask me about it! You will save time and it will allow me to fix the bug or update the documentation.
I'm looking forward to any kind of feedback, so please feel free to harass me =) (arthur.bit-monnot@laas.fr)



