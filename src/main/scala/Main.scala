import java.security.Security
import java.time.LocalDateTime
import java.util.Collections

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.google.auth.oauth2.GoogleCredentials
import com.typesafe.config.ConfigFactory
import io.grpc.auth.MoreCallCredentials
import java.util.Collections
import java.util.concurrent.TimeUnit

import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.google.protobuf.ByteString
import com.google.pubsub.v1
import com.google.pubsub.v1._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.concurrent.duration._
import akka.actor.ActorSystem
import org.conscrypt.Conscrypt

object Main extends App {

  val conscryptProviderPosition = 1
  Security.insertProviderAt(Conscrypt.newProvider, conscryptProviderPosition)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  lazy val gcpPubSub = new CloudPubSub("disco-sdp-dev")

  val fieldToBeMasked = "col1"
  val valueToBeMasked = "val1"
  val currTime = LocalDateTime.now().toString

  val sourceJson = List(s"""{"$fieldToBeMasked": "$valueToBeMasked", "col2": "$currTime"}""")

  val receivedDataFuture =
    gcpPubSub.pull("integrated-test-subscription", "disco-sdp-dev", sourceJson.size + 1)

  val published = Await.result(gcpPubSub.publish("projects/disco-sdp-dev/topics/integrated-test", sourceJson.head.getBytes), 5 seconds)
  println(s"Message published? $published")

  val genericRecord = Await.result(receivedDataFuture, 60 seconds)
  println(s"Records received: $genericRecord")

  system.terminate()
  System.exit(0)
}
