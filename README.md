# FAPE: Flexible Acting and Planning Environment

[![Build Status](https://travis-ci.org/arthur-bit-monnot/fape.svg?branch=master)](https://travis-ci.org/arthur-bit-monnot/fape)


## FAPE from source

Build process requires SBT and Java 8. Instructions for installing SBT can be found [here](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html). FAPE is
developed and tested on Linux but there should be no problems in getting it to run on another OS.

When you have SBT, simply run the following command in the project directory.

    # Build the project with SBT
    sbt compile

    # Create executable script
    sbt pack

You can generate a script to run the FAPE planner with `sbt pack`.
This will produce an executable at `target/pack/bin/fape`.
To see options, run `fape --help`.


### IDE support

We recommend using IntelliJ Idea which has support for SBT project
definitions (either built-in or though plugin depending on the version
you have).

We use annotations from project lombok to generate some boiler plate code.
This should not interfere with sbt build but you  should enable it in IntelliJ:

 - add the lombok plugin
 - enable annotation processing:
    Preferences -> Build, Execution, Deployment -> Compiler ->
     Annotation Processors -> Enable annotation processing


## Run

Once you have compiled FAPE (using sbt pack), you can run it with

    path/to/fape path/to/anml/problem/file
    # e.g. target/pack/bin/fape planning/domains/logistics-hier/logistics.x04-1.pb.anml

For more options, run `fape --help`.
Example ANML planning problems are available in `planning/domains`.



## Precompiled Versions

You can find precompiled version of FAPE at: https://github.com/arthur-bit-monnot/fape/releases

Precompiled binaries come in two flavors:

 - an executable script for Linux (e.g. `fape-1.0`) that is self contained with all dependencies 
 - a single jar bundled with all dependencies (e.g. `fape-planning-assembly-1.0.jar`). This can be run with 
    `java -ea -jar fape-planning-assembly-1.0.jar [options] path/to/problem.anml`

In both case, your version of the Java Runtime Environment must be at least 1.8.

Compiled jars for FAPE are also available on [Maven Central Repository](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.arthur-bit-monnot%22)



