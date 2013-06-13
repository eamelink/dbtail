name := "dbtail"

scalaVersion := "2.10.2"

libraryDependencies ++= List(
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "mysql" % "mysql-connector-java" % "5.1.25",
  "com.github.scopt" %% "scopt" % "3.0.0")
  
resolvers += "sonatype-public" at "https://oss.sonatype.org/content/groups/public"