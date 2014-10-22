% FAPE: User Manual
% Arthur Bit-Monnot
%

<!--
Note: you can use Pandoc to generate this document:
# pandoc --standalone --listing -N -o README.pdf README.md

Otherwise it should be readable as plain text as well.
-->

<!-- This is simply to put the ANML blocks into boxes when generating the PDF -->
\lstset{language=c++}
\lstset{basicstyle=\ttfamily\small,breaklines=true}
\lstset{morekeywords={action,start,end,all,contains,decomposition}}
\lstset{frame=single}
\lstset{keywordstyle=\bfseries}


This document will give a brief introduction to FAPE.
Its objective is merely to give pointers and some keys on what is currently supported in FAPE and give a rough idea of how things work internally.

Until a more detailed and formal description of the planner is written, we hope it will allow you to start playing around with FAPE without to much pain.
We are aware that this document stays very high level and that it won't answer all question that might arise when using (or worse, contributing to) FAPE. Please ask any question you have, I will be glad to answer it!



# ANML

The objective of this section to describe which subset of ANML is supported in FAPE, assuming the reader already knows the language.
If you are not familiar with ANML, you should first have a look at the ANML manual.

Several examples of valid ANML domains are given in the `domains/` directory.

## Types

    // Defines a type with no parent and no attributes
    type Location;
    
    // Defines a type NavLocation that is a subclass of Location
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
could rewrite the `connected` function above to enforce that the value doesn't
change over time: 

    // a constant is a function whose value doesn't change over time
    constant boolean connected(Location a, Location b);


A facility is provided to define functions in type. The two following ANML
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

**WARNING**: in its current implementation, the `contains` keyword differs from the mainline ANML definition since it does *not* require the condition to be false before and after the interval. In fact, the above statement is equivalent to `[start,end] contains [1] s`;


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



        action Pick(Robot r, Item i) {
          :decomposition{ [all] PickWithLeftGripper(r, i); };
          :decomposition{ [all] PickWithRightGripper(r, i); };
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
    end(idA) < start(idB) -10;
    
    // specifies that the second statement must end exactly 60 time units before
    // the end of the containing interval (i.e. the action if the statement is defined
    // in an action or the plan if the statement is defined in the problem).
    end(idB) = end -60;


It is also possible to give temporal constraints between actions appearing in a decomposition. The `ordered` and `unordered` keywords are not supported but can be replaced by temporal constraints as in the following example.

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

To put things short: without task conditions, FAPE is a temporal planner, searching in plan-space.


## Subtasks

ANML allows to define subtasks. The following will be true iff there is an action Pick parameterized with bob, red_cup and kitchen in the given interval.

    [all] contains Pick(Bob, red_cup, kitchen);

FAPE currently has two ways of dealing with such task conditions: one inheriting from the HTN way and one from the classical planning way.

### HTN Way

The HTN way is to insert an action with the given parameters in the partial-plan whenever a task condition appears.
Like in HTN, one needs to provide a root action from which the others would be derived.

In addition to that, actions might be inserted as standalone resolvers (e.g. to solve an open goal flaw). Hence actions in the plan are not necessarily in the tree derived from the root action.
To avoid that, the `motivated` keyword for an action enforce that an action must appear as a task condition of a higher-level action (i.e. it can not be inserted as a standalone resolver).

Making all actions motivated and giving a root action will result in a HTN-like search, expending a search tree from the root action.

This setting is the default. It corresponds to the option `-p base+dtg`


### Task conditions

The other way supported in FAPE is to consider a subtask statement as a task condition.

It is considered true if there is an action whose time-points and parameters can be unified with those of the task condition.

Hence an opened task condition is a flaw which can be solved by adding a support link between this task condition and an action (which is either added or already in the plan).
Also, a unique action might support several task conditions.

    predicate hasCar();
    [start] hasCar := false;
    
    action RentCar() {
      get : GetCar();
      ret : ReturnCar();
      end(get) < start(ret);
    };

    action GetCar() {
      motivated;
      hasCar == false :-> true;
    };

    action ReturnCar() {
      motivated;
      hasCar == true :-> false;
    };


Lets assume we have the above ANML domain and an empty partial-plan with an open-goal `hasCar == true`.

 - The only resolver for this flaw is to insert an action `GetCar()` supporting the condition.
 - However `GetCar` is marked as `motivated`: it must be a subtask of some other task.
   This is a flaw for which the only resolver is to:
     - add an action `RentCar()`
     - Add a support link stating that the `GetCar` condition in `RentCar()` is supported
       by our instance of `GetCar()`
 - Our instance of `RentCar` still has another unsupported task condition: `ReturnCar`.
   Since there is no instance of a `ReturnCar` action in the plan, the only resolver for this
   flaw is to add an instance of `ReturnCar()` and set it as supporting the task condition.
   If an instance of `ReturnCar` was already in the partial-plan, an alternative resolver
   would have been to set this instance as supporting the task condition.

The behaviour is enabled with option `-p taskcond`.

Its is the richer way of combining hierarchies with traditional goals. However it still lacks good formalization and adapted search strategies.


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


Similarly, subtasks in the domain definition will result (depending on the option -p) in
the addition of an action (that must be decomposed and/or have some conditions) or in the addition of an opened task conditions.

    // example of objective tasks
    
    // the PR2 must clean the table
    CleanTable(PR2);

    // the PR2 must give the cup before time 100
    [start, 100] contains Give(PR2, Arthur, cup);

    // any robot must transport the coffee cup from the kitchen to the dining room
    constant Robot r;
    Transport(r, coffee_cup, kitchen, dining_room);



## Search and scalability

### Search: algorithm and strategies

FAPE uses the PSP (Plan Space Planning) algorithm as a search.

    PSP(p):
      flaws <- getFlaws(p)
      if flaws is empty
        return p // solution plan

      select any flaw f in flaws
      resolvers <- GetResolvers(f, p)

      if resolvers is empty
        return failure

      non-deterministically choose a resolver r
      return PSP( Apply(r, p) )
    
The two decisions to be made in this algorithm are:

 - which flaw to solve in the current partial plan
 - which partial plan (or which resolver) to consider for the next iteration.

Those are respectively call *flaw selection strategy* and *plan selection strategy*.
The one used in FAPE come from the venerable heuristic of lifted plan-space planning.

The default *flaw selection strategy* is a least committing first: the chosen flaw is the one with least number of resolvers.

The default *plan selection strategy* considers the number of flaws and the number of actions in the plan (lower is better). This results in a best first search in the space of partial plans.

Other strategies are implemented in the package `core.planning.search.strategies`. Those can be tested through the `--strats` option.

### Scalability

You may have noticed that the above strategies are not informed: they only look at the current partial plan without making a guess at the distance towards the goal (the number flaws is only a very weak information since much more will arise). As a consequence, the scalability of FAPE is very limited in the number of actions.

Some domain independent heuristics are considered for the future.
Meanwhile you can:

 - carefully design a hierarchical domain to guide the search
 - write a domain-dependant heuristic



**Efficiency:** Note that the JVM (and especially scala bytecode) can be very slow to warm up. If It seems like the planner take an incredibly large time to process few states, try to warm it up first (the option `-n` allows to set a number of repetitions for the run).
