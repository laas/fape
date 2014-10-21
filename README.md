 

\lstset{language=c++}
\lstset{basicstyle=\ttfamily\small,breaklines=true}
\lstset{morekeywords={action,start,end,all,contains,decomposition}}
\lstset{frame=single}
\lstset{keywordstyle=\bfseries}


# ANML

The objective of this section to describe which subset of ANML is supported in FAPE, assuming the reader already knows the language.
If you are not familiar with ANML, you should first have a look at the ANML manual.

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
    
    // Defines a type Item and a function "Location Item.location(Item ?)"
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

Constraints can be expressed 

## Resources

Resources are not supported yet. An initial implementation is currently in the source repository but is not stable enough for daily usage.
