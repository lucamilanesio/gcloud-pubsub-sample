import java.nio.charset.{Charset, StandardCharsets}
import java.security.Security

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.googlecloud.pubsub.grpc.scaladsl.{GooglePubSub, GrpcSubscriberExt}
import akka.stream.scaladsl.{Sink, Source}
import com.google.protobuf.ByteString
import com.google.common.base.Utf8
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.pubsub.{GetSubscriptionRequest, PublishRequest, PublishResponse, PubsubMessage, ReceivedMessage, StreamingPullRequest, Subscription}
import com.typesafe.config.ConfigFactory
import org.conscrypt.Conscrypt

import scala.collection.immutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Try
//import com.google.api.gax.rpc.ApiException
//import com.google.cloud.pubsub.v1.SubscriptionAdminClient
//import com.google.pubsub.v1.{ProjectSubscriptionName, ProjectTopicName, PushConfig, Subscription}

object Main extends App {
  Try {
    val conscryptProviderPosition = 1
    Security.insertProviderAt(Conscrypt.newProvider, conscryptProviderPosition)
  }

  val config = ConfigFactory.parseString("alpakka.google.cloud.pubsub.grpc.rootCa = \"ca-certificates.crt\"")
  implicit val system = ActorSystem("test", config)
  implicit val materializer = ActorMaterializer()
  val topicName = "integrated-test"
  val projSubName = ProjectSubscriptionName.of("disco-sdp-dev", s"$topicName-subscription").toString

  println("Running PubSub Sample ... ")

  val sourceSubscription = Await.result(GrpcSubscriberExt().subscriber.client
    .getSubscription(GetSubscriptionRequest(projSubName)), 30 seconds)

  println(s"Subscribed to topic ${projSubName}")

  val streamingPullRequest = StreamingPullRequest()
    .withSubscription(projSubName)
    .withStreamAckDeadlineSeconds(30000)

  def publishAllMessages(subscription: Subscription, messages: List[ByteString]): Future[immutable.Seq[PublishResponse]] = {
    val publishMessages = messages.map(msg => PubsubMessage().withData(msg))

    Source(publishMessages).map { msg =>
      PublishRequest()
        .withTopic(subscription.topic)
        .addMessages(msg)
    }.via(GooglePubSub.publish(parallelism = 1))
      .runWith(Sink.seq)
  }

  val publishResult = Await.result(publishAllMessages(sourceSubscription, List(ByteString.copyFrom("message", StandardCharsets.UTF_8))), 30 seconds)
  println(s"Publish finished: ${publishResult}")

  val subscriber = GooglePubSub
    .subscribe(streamingPullRequest, 30 seconds)

  val result = Await.result(subscriber
    .take(1)
    .takeWithin(30 seconds)
    .runWith(Sink.seq[ReceivedMessage]), 30 seconds)

  println(s"Done: ${result} ")

  system.terminate()

//  println("Running PubSub Sample ... ")
//
////  val projectId = "disco-sdp-dev"
//  val projectId = "disco-sdp-dev"
//
//  // Your topic ID, eg. "my-topic"
//  val topicId = "integrated-test"
//
//  // Your subscription ID eg. "my-sub"
//  val subscriptionId = s"$topicId-subscription"
//
//  val topicName = ProjectTopicName.of(projectId, topicId)
//
//  // Create a new subscription
//  val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)
//  try {
//    val subscriptionAdminClient = SubscriptionAdminClient.create
//    try { // create a pull subscription with default acknowledgement deadline (= 10 seconds)
//      val subscription = subscriptionAdminClient.getSubscription(subscriptionName)
//      println(s"Subscription ${subscriptionName.getProject}:${subscriptionName.getSubscription} retrieved: ${subscription}")
//    } catch {
//      case e: ApiException =>
//        e.printStackTrace()
//        // example : code = ALREADY_EXISTS(409) implies subscription already exists
//        println(s"Status Code: ${e.getStatusCode}")
//    } finally if (subscriptionAdminClient != null) subscriptionAdminClient.close()
//  }
}
