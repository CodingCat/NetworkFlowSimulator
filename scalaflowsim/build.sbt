name := "scalaflowsim"

version := "1.0"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

parallelExecution in Test := false
