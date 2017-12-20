import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val finagle = "com.twitter" %% "finagle-core" % "7.0.0"
  lazy val kamonCore = "io.kamon" %% "kamon-core" % "1.0.0-RC4"
}
