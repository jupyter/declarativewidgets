/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */
name := "urth-widgets"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "com.ibm.spark" %% "kernel" % "0.1.2-SNAPSHOT" % "provided",
  "com.ibm.spark" %% "kernel-api" % "0.1.2-SNAPSHOT" % "provided",
  "com.ibm.spark" %% "protocol" % "0.1.2-SNAPSHOT" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test", // Apache v2
  "org.scalactic" %% "scalactic" % "2.2.0" % "test", // Apache v2
  "org.mockito" % "mockito-all" % "1.9.5" % "test"   // MIT
)
