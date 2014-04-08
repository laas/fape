# FAPE

## Build

FAPE comes with a maven build file `pom.xml` that should be understood by most
IDE. Furthermore, you can use maven2 directly to build the project:

    # Build the project with Maven
    mvn compile


    # Build the project
    sbt compile


## Run

The main entry for pure planning is the class fape.Planning.

    # Execute the Planner class with "problems/handover.anml" as argument.
    mvn exec:java -Dexec.mainClass="fape.Planning" -Dexec.args="problems/handover.anml"
    mvn exec:java -Dexec.mainClass="fape.Planning" -Dexec.args="--help"
    
    # With SBT
    sbt "runMain fape.Planning problems/handover.anml"
    sbt "runMain fape.Planning --help"
    

There is currently two planners that differs on what they use for domain
analysis. To compare them on one problem:

    sbt "runMain fape.Planning --quiet --planner base,rpg problems/handover.anml problems/Dream3.anml"

Remember that java can be very slow to warm up. You can specify how many times
you want a planner to run on the same problem by using the -n option. The
following command line will run the rpg planner (that using relax planning
graphs) 50 times on the domain problems/handover.anml:

    sbt "runMain fape.Planning --quiet -n 50 --planner rpg problems/handover.anml"

## Dependencies

FAPE uses some external dependencies for ANML parsing, Simple Temporal
Networks and graphs.
Those can be found here : [](https://github.com/planstack)
