# FAPE: Flexible Planning and Acting Environment


## Building FAPE

Build process requires SBT. Instructions for installing SBT can be found [here](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html).

    # Build the project with SBT
    sbt compile

You can generate a script to run the FAPE planner with `sbt pack`.
This will produce an executable at `target/pack/bin/fape`.
To see options, run `fape --help`.



### IDE support

We recommend using IntelliJ Idea which has support for SBT project
definitions (either built-in or though plugin depending on the version
you have).

## Run

Once you have compiled FAPE (using sbt pack), you can run it with

    path/to/fape path/to/anml/problem/file
    # e.g. target/pack/bin/fape domains/logistics-hier/logistics.x04-1.pb.anml

For more options, run `fape --help`



