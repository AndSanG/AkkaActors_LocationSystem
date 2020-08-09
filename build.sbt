name := "AkkaActors_LocationSystem"

version := "0.1"

scalaVersion := "2.13.1"

val akkaVersion = "2.6.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "org.iq80.leveldb" % "leveldb" % "0.10", //required for backend of akka-persistence-query
  "com.twitter" %% "chill" % "0.9.1",
  "com.twitter" %% "chill-akka" % "0.9.1",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
