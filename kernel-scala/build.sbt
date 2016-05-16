/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
name := "urth-widgets"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

val sparkVersion = "1.6.1"
val toreeVersion = "0.1.0.dev6-incubating-SNAPSHOT"

resolvers +=  "Apache Maven Repository" at "https://repository.apache.org/content/repositories/snapshots"

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.apache.toree" %% "toree-kernel" % toreeVersion % "provided",
  "org.apache.spark" %% "spark-core" % sparkVersion  % "provided",
  "org.apache.spark" %% "spark-repl" % sparkVersion  % "provided",
  "org.apache.spark" %% "spark-core" % sparkVersion  % "test" classifier "tests",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test", // Apache v2
  "org.scalactic" %% "scalactic" % "2.2.0" % "test", // Apache v2
  "org.mockito" % "mockito-all" % "1.9.5" % "test"   // MIT
)

ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet

