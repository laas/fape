name := "fape"

version := "0.1"

scalaVersion := "2.10.3"

crossPaths := false

resolvers += "planstack-maven" at "http://planstack.github.io/repository/maven"

libraryDependencies += "planstack" % "planstack-constraints" % "0.3"

libraryDependencies += "planstack" % "planstack-graph" % "0.3"

libraryDependencies += "planstack" % "planstack-anml" % "0.3.1"

javaOptions in run += "-Xmx3000m"

javacOptions ++= Seq("-source", "1.7")

mainClass in (Compile, run) := Some("fape.core.planning.Planner")


pomExtra :=
<build>
  <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.1</version>
        <configuration>
          <source>1.7</source>
          <target>1.7</target>
        </configuration>
      </plugin>
    </plugins>
</build>