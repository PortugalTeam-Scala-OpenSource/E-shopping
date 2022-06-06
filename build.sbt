val scala3Version = "3.1.2"

lazy val infrastructure = project
lazy val example = project.dependsOn(infrastructure)
