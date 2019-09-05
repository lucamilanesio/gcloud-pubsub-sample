

import java.util.Collections

import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import com.google.protobuf.ByteString
import com.google.pubsub.v1._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.reflectiveCalls
import scala.concurrent.duration._
import FutureUtils._
import akka.actor.ActorSystem
import com.google.auth.oauth2.GoogleCredentials
import com.typesafe.config.ConfigFactory
import io.grpc.auth.MoreCallCredentials

class CloudPubSub(projectId: String)(implicit ec: ExecutionContext, m: ActorMaterializer) {
  object PubsubUtils {

    def projectTopicNameOf(projectId: String, topic: String) =
      s"projects/$projectId/subscriptions/$topic"

    def projectSubscriptionNameOf(projectId: String, subscription: String) =
      s"projects/$projectId/subscriptions/$subscription"
  }


  val config = ConfigFactory.parseString("alpakka.google.cloud.pubsub.grpc.rootCa = \"ca-certificates.crt\"")
  implicit val system = ActorSystem("test", config)

  val host = "pubsub.googleapis.com"
  val port = 443

  val settings =
    GrpcClientSettings
      .connectToServiceAt(host, port)
      .withCallCredentials(
        MoreCallCredentials.from(
          GoogleCredentials.getApplicationDefault.createScoped(
            Collections.singletonList("https://www.googleapis.com/auth/pubsub")
          )
        ))

  implicit val materializer = ActorMaterializer()(system)
  def publish(topicId: String, firstMessage: Array[Byte], extraMessages: Array[Byte]*): Future[Boolean] = {
    val publisher = PublisherClient.create(settings, materializer, ec)
    val messagesToPublish = Seq(firstMessage) ++ extraMessages
    val publishMessages = messagesToPublish.map { msg =>
      val publishMessage = PubsubMessage
        .newBuilder()
        .setData(ByteString.copyFrom(msg))
        .build()

      PublishRequest
        .newBuilder()
        .addMessages(publishMessage)
        .setTopic(topicId)
        .build()
    }

    Future
      .traverse(publishMessages) { pm =>
        publisher.publish(pm).toCompletableFuture().toScala(30 seconds)
      }
      .map(_.size == messagesToPublish.size)
  }

  def pull(subscriptionId: String, projectId: String, maxMessages: Int): Future[Seq[ByteString]] = {
    val subscriptionName = s"projects/$projectId/subscriptions/integrated-test-subscription"
    val subscriber = SubscriberClient.create(settings, materializer, ec)

    val request = PullRequest
      .newBuilder()
      .setSubscription(subscriptionName)
      .setMaxMessages(maxMessages)
      .build()
    subscriber
      .pull(request)
      .toCompletableFuture()
      .toScala(60 seconds)
      .map { response =>
        response.getReceivedMessagesList().asScala.toList.map(_.getMessage.getData)
      }

  }

  private def topicNameOf(topicName: String) = PubsubUtils.projectTopicNameOf(projectId, topicName)

  private def autoClose[T <: { def close() }, R](objToClose: T)(functionBodyForObj: T => R) =
    try {
      functionBodyForObj.apply(objToClose)
    } finally {
      objToClose.close()
    }

  private def autoShutdown[T <: { def shutdown() }, R](objToClose: T)(functionBodyForObj: T => R) =
    try {
      functionBodyForObj.apply(objToClose)
    } finally {
      objToClose.shutdown()
    }

  //  private def ackMessages(subscriber: GrpcSubscriberStub, subscriptionName: String, messages: mutable.Buffer[ReceivedMessage]) =
  //    subscriber.acknowledgeCallable.call {
  //      val ackIds = messages.map(_.getAckId)
  //      AcknowledgeRequest.newBuilder
  //        .setSubscription(subscriptionName)
  //        .addAllAckIds(ackIds.asJava)
  //        .build
  //    }
}

object CloudPubSub {

  implicit class PubSubTimeout(duration: Duration) {
    def asDuration = duration
  }
}
