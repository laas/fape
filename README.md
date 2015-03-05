# FAPE: Flexible Planning and Acting Environment


## Building FAPE

Build process supports both SBT (build.sbt) and maven2 (pom.xml).
We recommend that you use SBT since it will be used as well for some
of FAPE's dependencies.
Instructions for installing SBT can be found [here](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html).

    # Build the project with SBT
    sbt compile

You can generate a script to run the FAPE planner with `sbt pack`.
This will produce an executable at `target/pack/bin/fape-planner`.
To see options, run `fape-planner --help`.



### IDE support

Most IDEs should understand the `pom.xml` file. We recommend to use [IntelliJ-IDEA](http://www.jetbrains.com/idea/download/) since it has the best scala support (needed for some FAPE's dependencies). You can generate a project configuration of IntelliJ by running the following sbt command: `sbt gen-idea`.


## Run

The main entry for pure planning is the class fape.Planning.

    # Run the planner in its default configuration to solve the given
    # handover problem.
    sbt "runMain fape.Planning ../domains/handover/handover.1.pb.anml --gui"

    # display command line options
    sbt "runMain fape.Planning --help"

    # You can do the same with maven:
    mvn exec:java -Dexec.mainClass="fape.Planning" -Dexec.args="../domains/handover/handover.1.pb.anml --gui"
    mvn exec:java -Dexec.mainClass="fape.Planning" -Dexec.args="--help"
    


Remember that the JVM can be very slow to warm up. You can specify how many times
you want a planner to run on the same problem by using the -n option. The
following command line will run the planner 50 times on the given problem.

    sbt "runMain fape.Planning --quiet -n 50 ../domains/handover/handover.1.pb.anml"

Exploring the available domains in `../domains/` and looking at the different options
listed in the help should be a good start for learning about FAPE.



IMPORTANT: FAPE extensivily uses assertions to make sure everything go as
expected. So please make sure that the JVM has assertions enabled when
running FAPE (JVM option -ea).
This is already activated for the SBT configuration file.
