val scala3Version = "3.1.2"

name := "infrastructure"
version := "0.1.0-SNAPSHOT"
scalaVersion := scala3Version
libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test

dependsOn(kafka)
dependsOn(actor)

lazy val kafka = project
lazy val actor = project
