import com.google.api.gax.grpc.ApiException
import com.google.api.resourcenames.ResourceName
import com.google.cloud.pubsub.spi.v1.SubscriptionAdminClient
import com.google.pubsub.v1.SubscriptionName
//import com.google.api.gax.rpc.ApiException
//import com.google.cloud.pubsub.v1.SubscriptionAdminClient
//import com.google.pubsub.v1.{ProjectSubscriptionName, ProjectTopicName, PushConfig, Subscription}

object Main extends App {

//  private def integratedTestAlpakka = {
//    val config = ConfigFactory.parseString("alpakka.google.cloud.pubsub.grpc.rootCa = \"ca-certificates.crt\"")
//    implicit val system = ActorSystem("test", config)
//
//    try {
//      Try {
//        val conscryptProviderPosition = 1
//        Security.insertProviderAt(Conscrypt.newProvider, conscryptProviderPosition)
//      }
//
//      implicit val materializer = ActorMaterializer()
//      val topicName = "integrated-test"
//      val projSubName = ProjectSubscriptionName.of("disco-sdp-dev", s"$topicName-subscription").toString
//
//      println("Running PubSub Sample ... ")
//
//      var success = false
//      var retryCount = 0
//      var sourceSubscription: Option[Subscription] = Option.empty
//
//      while (!success && retryCount < 10) {
//        try {
//          sourceSubscription = Some(Await.result(GrpcSubscriberExt().subscriber.client
//            .getSubscription(GetSubscriptionRequest(projSubName))
//            , 30 seconds))
//          success = true
//        } catch {
//          case t: Throwable => t.printStackTrace()
//            val sleepTime = Random.nextInt(3000)
//            println(s"Sleeping ${sleepTime / 1000} secs")
//            Thread.sleep(sleepTime)
//            retryCount += 1
//        }
//      }
//
//      require(sourceSubscription.isDefined, s"Unable to subscribe to topic ${projSubName}")
//
//      println(s"Subscribed to topic ${projSubName}")
//
//      val streamingPullRequest = StreamingPullRequest()
//        .withSubscription(projSubName)
//        .withStreamAckDeadlineSeconds(30000)
//
//      def publishAllMessages(subscription: Subscription, messages: List[ByteString]): Future[immutable.Seq[PublishResponse]] = {
//        val publishMessages = messages.map(msg => PubsubMessage().withData(msg))
//
//        Source(publishMessages).map { msg =>
//          PublishRequest()
//            .withTopic(subscription.topic)
//            .addMessages(msg)
//        }.via(GooglePubSub.publish(parallelism = 1))
//          .runWith(Sink.seq)
//      }
//
//      val publishResult = Await.result(publishAllMessages(sourceSubscription.get, List(ByteString.copyFrom("message", StandardCharsets.UTF_8))), 30 seconds)
//      println(s"Publish finished: ${publishResult}")
//
//      val subscriber = GooglePubSub
//        .subscribe(streamingPullRequest, 30 seconds)
//
//      val result = Await.result(subscriber
//        .take(1)
//        .takeWithin(30 seconds)
//        .runWith(Sink.seq[ReceivedMessage]), 30 seconds)
//
//      println(s"Done: ${result} ")
//    } finally {
//      system.terminate()
//    }
//  }

    println("Running PubSub Sample ... ")

  //  val projectId = "disco-sdp-dev"
    val projectId = "disco-sdp-dev"

    // Your topic ID, eg. "my-topic"
    val topicId = "integrated-test"

    // Your subscription ID eg. "my-sub"
    val subscriptionId = s"$topicId-subscription"

//    val topicName = ProjectTopicName.of(projectId, topicId)

    // Create a new subscription
//    val subscriptionName: ResourceName = ProjectSubscriptionName.of(projectId, subscriptionId)
      val subscriptionName: SubscriptionName = SubscriptionName.create(projectId, subscriptionId)

  try {
      val subscriptionAdminClient = SubscriptionAdminClient.create
      try { // create a pull subscription with default acknowledgement deadline (= 10 seconds)
        val subscription = subscriptionAdminClient.getSubscription(subscriptionName)
        println(s"Subscription ${subscriptionName.getProject}:${subscriptionName.getSubscription} retrieved: ${subscription}")
      } catch {
        case e: ApiException =>
          e.printStackTrace()
          // example : code = ALREADY_EXISTS(409) implies subscription already exists
          println(s"Status Code: ${e.getStatusCode}")
      } finally if (subscriptionAdminClient != null) subscriptionAdminClient.close()
    }
}
