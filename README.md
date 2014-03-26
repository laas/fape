# ANML Parser

Implementation of an ANML parser using scala combinator parsers.

It provides a full model of an ANML problem that should be readily usable from
any JVM planner.
The library itself is written in Scala and uses scala collections. 
Methods are provided for easy integration into a Java planner: methods
prepended with `j` returns java collections instead of scala ones.

Documentation for this project can be accessed through
[Javadoc](http://planstack.github.io/repository/api/java/anml/) or
[Scaladoc](http://planstack.github.io/repository/api/scala/anml/).

## Usage 

Build is done with [SBT](http://www.scala-sbt.org/).

To use it as a dependency on your project, add the following to your `pom.xml`

```
<dependencies>
    ...
    <dependency>
        <groupId>planstack</groupId>
        <artifactId>planstack-anml</artifactId>
        <version>0.2-SNAPSHOT</version>
    </dependency>
</dependencies>
<repositories>
    ...
    <repository>
        <id>planstackmaven</id>
        <name>planstack-maven</name>
        <url>http://planstack.github.io/repository/maven/</url>
        <layout>default</layout>
    </repository>
</repositories>
```

You can also find jars [here](http://planstack.github.io/repository/maven/)
but we recommend you to use maven to keep track of updates.

## ANML

This gives a quick introduction to the subset of ANML that is supported
by this parser. 
If you are not familiar with ANML, you should first have a look a the ANML manual.

### Types

```
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
```

### Functions

```
function boolean connected(Location a, Location b);
```

The above ANML block defines a function taking two parameters of type
`Location` and whose return value is of type `boolean`. Functions have an
implicit time parameter (i.e. the value of `connected(Kitchen, Entrance)`
might not be the same during the whole plan).

Several other syntaxes can be used to define functions:

```
// a variable is a function with no parameters
variable Room houseCenter; // is the same as
function Room houseCenter();

// a predicate is a function whose value type is boolean
predicate connected(Location a, Location b); // is the same as
function boolean connected(Locatin a, Location b);
```

ANML further allows to define functions whose value is constant over time. We
could rewrite the `connected` function above to enforce that the value doesn't
change over time: 

```
// a constant is a function whose value doesn't change over time
constant boolean connected(Location a, Location b);
```

A facility is provided to define functions in type. The two following ANML
blocks are equivalent:

```
type Robot with {
  function boolean canGo(Location l);
};
```

```
type Robot;
function boolean Robot.canGo(Robot r, Location l);
```

Given a `r` instance of `Robot` (or any of its subclasses), writing
`r.canGo(l)` is equivalent to writing `Robot.canGo(r, l)`.



### Logical Statements

A logical statement describes changes and conditions on state variables. It is
associated with a start and en time point.
They refer to state variables: ANML functions with parameters.

 - Persistence: `connected(Kitchen, Entrance) == true;` requires the state
   variable `connected(Kitchen, Entrance)` to be true between the start and
   end time points of the statement.
 - Assignment: `connected(Kitchen, Entrance) := true;` specifies that the
   state variable `connected(Kitchen, Entrance)` will have the value true at
   the end of the statement and is undefined between start and end.
 - Transition: `connected(Kitchen, ENtrance) == false :-> true` requires the
   state variable to have the value false at the start of the statement and
   specifies that it will have value `false` at the end of the statement.
   
   
### Temporal annotations

Temporal annotations are temporal constraints on the timepoints of
statements. Given a statement `s`, where `start(s)` and `end(s)` represent its
start and end time-points.

```
[all] s; or [start,end] s;    => start(s) == start && end(s) == end
[start+10, end-5] s;            => start(s) == start +10 && end(s) == end-5
[2, 10] s;                    => start(s) == 2 && end(s) == 10;
// or any combination of the above annotations
```

In the preceding text, `start` and `end` refer to the start and end time
points of the interval containing the annotated statement (such as an action
or a problem).

It is possible to give the same annotation to several statements:

``` 
[start, end] {
  connected(a, b) == true;
  r.canGo(a) := false;
};
// is equivalent to
[start, end] connected(a, b) == true;
[start, end] r.canGo(a) := false;
```


A temporal annotation with the `contains` keyword means that the given
statement must be included in the interval:

```
[start, end] contains s;   
  => start(s) > start && end(s) < end
```

### Actions

Action are operators having typed parameters and that might contain any number
of statements. In the following example, the transition statement's start and
end timepoints are equals to those of the action.

```
action Move(Robot r, Location a, Location b) {
  [start, end] {
    r.location == a :-> b;
  };
};
```

Furthermore, actions can contain decompositions with the following syntax:

```
:decomposition { Move(r, a, b); };
:decomposition{ 
  ordered( 
    Move(PR2, Kitchen, anywhere), 
    Move(PR2, anywhere, LivingRoom) );
};
:decomposition{
  unordered(
    doThat(...),
    doThis(...) );
};
```

The `ordered` keyword in the second decomposition forces the first Move to be
ended before the second can start.


### Temporal Constraints

Temporal constraints can be specified between intervals (i.e. any ANML object
with start and end time-points such as actions or statements).
The interval must be given a local ID:

```
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
```

It is also possible to give temporal constraints between actions appearing in
a decomposition: 

```
action PickAndPlace(Robot r, Item i) {
  :decomposition {
    pickID : Pick(r, i);
    placeID : Place(r, i);
    end(pickID) < start(placeID);
  };
};
```

### Resources

Initial support for resources is introduced. 
It is currently limited to integer: left term *must* be a function with integer 
value and the right left *must* be an integer literal.

Those are valid resource statements:

```
[start] {
id : energy >= 50;
energy() > 50;
energy(robot) < 50;
energy <= 50;
robot.battery := 1;
energy :use 10;      //start: -10 end: +10
energy :consume 10;  // -10
energy :lend 10      // start: +10 end: -10
energy :produce 10;  // +10
};
```





## Implementation

The package `planstack.anml.parser` contains the parser itself and the
Abstract SYntax Tree that results from parsing.

The package `planstack.anml.model` contains classes that are to be used inside
an ANML planner.

## ANML Model

A model makes the distinction between two types of ANML objects: abstract
and concrete.

### Abstract Model

An abstract action defined by the ANML string:

```
action Move(Robot r, Location a, Location b) {
  [all] { r.location == a :-> b; };
};
```

is an abstract action containing abstract statements on the local variables r, a and
b. 
The scope of those variables is limited to the action Move.
They are not defined yet and will only be upon action instanciation.


### Concrete model

Classes in the concrete package refers to instanciated ANML objects.

Instanciating the move action with `moveId : Move(PR2, Kitchen, Entrance)` would result
in a concrete Action containing the concrete statement 
`[start(moveId), end(MoveId)] PR2.location == Kitchen :-> Entrance`.

Hence the transition from abstract to concrete ANML objects is done by mapping
every local variable/time-point to a global unique variable.

### ANML Problem

An ANML problem (which results from parsing an ANML input) consists in both
abstract and concrete ANML objects.

Every action is given an abstract representation containing astract statements
and abstract decompositions.

Every statement is mapped to a concrete statement involving instances of the domain.
