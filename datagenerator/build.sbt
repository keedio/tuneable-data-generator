name := "datagenerator"

version := "0.1.0"

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

libraryDependencies += "io.dropwizard.metrics" % "metrics-ganglia" % "3.1.0"

libraryDependencies += "com.mchange" % "c3p0" % "0.9.5"