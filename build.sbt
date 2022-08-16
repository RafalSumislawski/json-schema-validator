
name := "json-validation-service"
organization := "io.sumislawski"
version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.8"

import Dependencies._
libraryDependencies ++= logging.all ++ cats.all ++ circe.all ++ http4s.all ++ Vector(fs2Io)

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-Ywarn-unused",
  "-Xfatal-warnings",
  "-language:higherKinds"
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
