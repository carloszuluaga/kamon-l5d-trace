import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "io.kamon",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "kamon-l5d-trace",
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.bintrayRepo("kamon-io", "releases"),
    resolvers += Resolver.bintrayRepo("kamon-io", "snapshots"),
    libraryDependencies += kamonCore,
    libraryDependencies += finagle,
    libraryDependencies += scalaTest % Test
  )
