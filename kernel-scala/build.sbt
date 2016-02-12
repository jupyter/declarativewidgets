/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
name := "urth-widgets"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

/*
  Toree is brought in as an unmanaged dependency
 */

val sparkVersion = "1.5.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion  % "provided",
  "org.apache.spark" %% "spark-repl" % sparkVersion  % "provided",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test", // Apache v2
  "org.scalactic" %% "scalactic" % "2.2.0" % "test", // Apache v2
  "org.mockito" % "mockito-all" % "1.9.5" % "test"   // MIT
)
