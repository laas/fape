# FAPE

## Build

FAPE comes with a maven build file `pom.xml` that should be understood by most
IDE. Furthermore, you can use maven2 directly to build the project:


    # Builds the project
    mvn compile

    # Execute the Planner class with "problems/handover.anml" as argument.
    mvn exec:java -Dexec.mainClass="fape.core.planning.Planner" -Dexec.args="problems/handover.anml"


## Dependencies

FAPE uses some external dependencies for ANML parsing, Simple Temporal
Networks and graphs.
Those can be found here : [](https://github.com/planstack)
