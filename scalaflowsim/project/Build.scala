import sbt._

object ScalaSimBuild extends Build {
  System.setProperty("logback.configurationFile", "./logback.xml")

  lazy val root = Project("scalaflowsim", file("."), settings = Project.defaultSettings)
}
