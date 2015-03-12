import AssemblyKeys._
import sbtassembly.Plugin._


name := "tuneable-data-generator"

version := "0.1.5-SNAPSHOT"

scalaVersion := "2.10.4"

seq(assemblySettings: _*)

organization in ThisBuild := "org.keedio.datagenerator"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature", "-language:postfixOps")

resolvers in ThisBuild ++= Seq(
  "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Maven Central" at "http://repo1.maven.org/maven2/",
  Resolver.mavenLocal
)

libraryDependencies in ThisBuild ++= Seq(
  "com.novocode" % "junit-interface" % "0.11" % "test->default",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3"
    exclude("io.netty","netty"),
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.joda" % "joda-convert" % "1.7",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

// Publish to local maven repository by default instead of to ivy.
// Useful if integrating with Maven builds
publishTo in ThisBuild := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))

publishMavenStyle in ThisBuild := true

val mysettings = assemblySettings ++ commonSettings ++
Seq(test in assembly := {},
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) ⇒
{
	case PathList(xs@_*) if xs.last endsWith "application.conf" => {MergeStrategy.concat}
        case PathList("META-INF", "spring.tooling") => {MergeStrategy.discard}
        case PathList("META-INF", "MANIFEST.MF") => {MergeStrategy.discard}
	case x => old(x)
}
}
/*  ,excludedJars in assembly := {
    val cp = (fullClasspath in assembly).value
    cp filter {_.data.getName == "log4j-1.2.17.jar"}
  }*/
)

lazy val root = project.in(file("."))
  .aggregate(
    common,
    datagenerator
  )
  .settings(aggregate in run := false)

lazy val commonSettings = net.virtualvoid.sbt.graph.Plugin.graphSettings

lazy val common = project

lazy val datagenerator = project.dependsOn(common).settings(commonSettings: _*).settings(mysettings: _*)

// Add any command aliases that may be useful as shortcuts
addCommandAlias("cc", ";clean;compile")

addCommandAlias("pt", ";clean;package;test")

mainClass in assembly := Some("org.keedio.datagenerator.Main")
