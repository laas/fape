# Graphs

This project aims at providing a graph library for use in automated planning.
The objective is to provide a common interface for different type of graph
(labeled/unlabeled, simple/multi and directed/undiricted). This allows having
common algorithms and export tools for those graphs.

Furthermore implementation focuses on being able to copy graphs efficiently
in order to reduce memory consumption of planners.

Documentation for this project can be accessed through
[Javadoc](http://planstack.github.io/repository/api/java/graph/) or
[Scaladoc](http://planstack.github.io/repository/api/scala/graph/).

## Usage 

Build is done with [SBT](http://www.scala-sbt.org/).

To use it as a dependency on your project, add the following to your `pom.xml`

```
<dependencies>
    ...
    <dependency>
        <groupId>planstack</groupId>
        <artifactId>planstack-graph</artifactId>
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
