ThisBuild / scalaVersion := "2.12.7"
ThisBuild / organization := "org.milanesio"

lazy val pubSubSample = (project in file("."))
  .settings(
    name := "PubSubSample",
    libraryDependencies ++= Seq(
        "com.lightbend.akka" %% "akka-stream-alpakka-google-cloud-pub-sub-grpc" % "1.1.1",
        "org.conscrypt"     % "conscrypt-openjdk-uber" % "1.4.2",

        //        "com.google.cloud" % "google-cloud-pubsub" % "1.89.0"
    )
)
