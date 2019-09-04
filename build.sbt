ThisBuild / scalaVersion := "2.12.7"
ThisBuild / organization := "org.milanesio"

lazy val pubSubSample = (project in file("."))
  .settings(
    name := "PubSubSample",
    libraryDependencies += "com.google.cloud" % "google-cloud-pubsub" % "1.89.0"
)
