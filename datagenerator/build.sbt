name := "datagenerator"

version := "0.1.0-SNAPSHOT"

mainClass := Some("org.keedio.datagenerator.Main")

resolvers in ThisBuild ++= Seq(
  "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Maven Central" at "http://repo1.maven.org/maven2/",
  Resolver.mavenLocal
)

libraryDependencies += "com.google.guava"                  % "guava"                   % "14.0"

libraryDependencies +=  "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.3.2"

libraryDependencies += "org.springframework" % "spring-test" % "3.2.13.RELEASE" % "test"
