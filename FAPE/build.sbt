name := "fape-planning"

organization := "fr.laas.fape"

version := "0.1.1"

libraryDependencies += "com.martiansoftware" % "jsap" % "2.1"

libraryDependencies += "jfree" % "jfreechart" % "1.0.13"

javaOptions in run += "-Xmx3000m"

javaOptions in run += "-ea"

mainClass in (Compile, run) := Some("fape.Planning")

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

