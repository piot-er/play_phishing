ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.example"

lazy val root = (project in file("."))
  .settings(
    name := "url-reputation-orchestrator",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor" % "1.1.5",
      "org.apache.pekko" %% "pekko-stream" % "1.1.5",
      "org.apache.pekko" %% "pekko-http"   % "1.1.0",
      "org.apache.pekko" %% "pekko-connectors-kafka" % "1.1.0",
      "io.circe" %% "circe-core"    % "0.14.7",
      "io.circe" %% "circe-generic" % "0.14.7",
      "io.circe" %% "circe-parser"  % "0.14.7",
      "ch.qos.logback" % "logback-classic" % "1.4.14",
      "com.github.ben-manes.caffeine" % "caffeine" % "3.1.8"
    ),
    Compile / mainClass := Some("com.example.urlrep.Main")
  )

import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.{MergeStrategy, PathList}

assembly / assemblyMergeStrategy := {
  case PathList("reference.conf")      => MergeStrategy.concat
  case PathList("version.conf")        => MergeStrategy.first
  case PathList("module-info.class")   => MergeStrategy.discard
  case x =>
    val old = (assembly / assemblyMergeStrategy).value
    old(x)
}
