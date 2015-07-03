name := "planning"

organization := "fr.laas.fape"

version := "0.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.martiansoftware" % "jsap" % "2.1",
  "jfree" % "jfreechart" % "1.0.13",
  "junit" % "junit" % "4.12" % "test",
  "fr.laas.fape" %% "graphs" % "0.8-SNAPSHOT",
  "fr.laas.fape" %% "anml-parser" % "0.8-SNAPSHOT",
  "fr.laas.fape" %% "constraints" % "0.8-SNAPSHOT",
  "fr.laas.fape" %% "svg-plot"    % "0.1-SNAPSHOT",
  "de.sciss" % "prefuse-core" % "1.0.0"
)

javaOptions in run ++= Seq("-Xmx3000m", "-ea")

fork in run := true

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

