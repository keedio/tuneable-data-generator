name := "common"

version := "0.1.6-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies += "com.google.guava"                  % "guava"                   % "14.0"

libraryDependencies +=  "junit" % "junit" % "4.11" % "test"

libraryDependencies += "org.springframework" % "spring-context" % "3.2.13.RELEASE"

libraryDependencies ++= Seq("org.apache.kafka" %% "kafka" % "0.8.2.2"
  exclude("com.sun.jdmk","jmxtools")
  exclude("com.sun.jmx","jmxri"),
  "com.typesafe.play" %% "play-iteratees" % "2.3.5",
  "com.github.lucarosellini" % "kafka.kryo.codec" % "1.0.6"
    exclude("org.apache.zookeeper","zookeeper")
    exclude("org.apache.kafka","kafka_2.10")
)

ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := false

unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "src/main/scala" )

unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "app" )