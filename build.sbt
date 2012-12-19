organization := "com.swells"

name := "Bohemian Appsody"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.2"

seq(webSettings :_*)

classpathTypes ~= (_ + "orbit")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra" % "2.1.1",
  "org.scalatra" % "scalatra-specs2" % "2.1.1" % "test",
  "org.json4s" %% "json4s-native" % "3.0.0",
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.1-seq",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.1-seq",
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
  "com.typesafe.akka" % "akka-actor" % "2.0.4",
  "com.typesafe.akka" % "akka-slf4j" % "2.0.4",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.7.v20120910" % "container",
  "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar"))
)

seq(Twirl.settings: _*)
