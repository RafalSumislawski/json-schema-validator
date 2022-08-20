import Dependencies._
import _root_.io.github.davidgregory084.DevMode

name := "json-validation-service"
organization := "io.sumislawski"
version := "0.1.0-SNAPSHOT"

scalaVersion := "2.13.8"

libraryDependencies ++= logging.all ++ cats.all ++ circe.all ++ http4s.all ++ scalaTest.all.map(_ % Test) ++
  Vector(fs2Io, jsonSchemaValidator)

ThisBuild / tpolecatDefaultOptionsMode := DevMode

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
