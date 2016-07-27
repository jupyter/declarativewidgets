/**
 * Copyright (c) Jupyter Development Team.
 * Distributed under the terms of the Modified BSD License.
 */

import scala.util.Properties
import scala.io.Source

organization := "org.jupyter"

name := "declarativewidgets"

version := {
  val releaseVersionPath = "/src_version/RELEASE_VERSION"
  if(new java.io.File(releaseVersionPath).exists) {
    val src = Source.fromFile(releaseVersionPath)
    val version = src.getLines().next()
    src.close
    val regex = "SNAPSHOT(.*)".r
    regex.replaceFirstIn(version, "SNAPSHOT")
  } else {
    "0.1-SNAPSHOT"
  }
}

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

parallelExecution in Test := false

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/jupyter-incubator/declarativewidgets</url>
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jupyter-incubator/declarativewidgets.git</url>
      <connection>scm:git:git@github.com:jupyter-incubator/declarativewidgets.git</connection>
    </scm>
    <developers>
      <developer>
        <id>dwidgets</id>
        <name>declarative widgets</name>
        <url>https://github.com/jupyter-incubator/declarativewidgets/graphs/contributors</url>
      </developer>
    </developers>)

val repoUsername  = Properties.envOrElse("REPO_USERNAME", "")
val repoPassword  = Properties.envOrElse("REPO_PASSWORD", "")

pgpPassphrase := Some(Properties.envOrElse("PGP_PASSPHRASE", "").toCharArray)

pgpSecretRing := file("/root/.gpg/secring.gpg")

credentials += Credentials("Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  repoUsername,
  repoPassword)