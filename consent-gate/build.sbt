ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.example"

lazy val root = (project in file("."))
  .settings(
    name := "consent-gate",
    libraryDependencies ++= Seq(
      // Pekko
      "org.apache.pekko" %% "pekko-actor" % "1.1.5",
      "org.apache.pekko" %% "pekko-stream" % "1.1.5",
      "org.apache.pekko" %% "pekko-connectors-kafka" % "1.1.0",

      // Logging
      "ch.qos.logback" % "logback-classic" % "1.4.14",

      // Circe
      "io.circe" %% "circe-core" % "0.14.7",
      "io.circe" %% "circe-generic" % "0.14.7",
      "io.circe" %% "circe-parser" % "0.14.7"
    ),

    Compile / mainClass := Some("com.example.consentgate.ConsentGateApp"),

    // plugin sbt-assembly do fat jar
    assembly / assemblyMergeStrategy := {
      case PathList("reference.conf") => MergeStrategy.concat
      case PathList("version.conf")   => MergeStrategy.first
      case PathList("module-info.class") => MergeStrategy.discard
      case x =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    }
  )
