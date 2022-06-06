val scala3Version = "2.13.1"

name := "actor"
version := "0.1.0-SNAPSHOT"
scalaVersion := scala3Version

val CompileOnly = config("compileonly")
ivyConfigurations += CompileOnly.hide

// appending everything from 'compileonly' to unmanagedClasspath
unmanagedClasspath in Compile ++=
  update.value.select(configurationFilter("compileonly"))
