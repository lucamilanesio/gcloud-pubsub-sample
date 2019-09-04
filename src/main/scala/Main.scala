import com.google.api.gax.rpc.ApiException
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.pubsub.v1.{ProjectSubscriptionName, ProjectTopicName, PushConfig, Subscription}

object Main extends App {

  println("Running PubSub Sample ... ")

//  val projectId = "disco-sdp-dev"
  val projectId = "disco-sdp-dev"

  // Your topic ID, eg. "my-topic"
  val topicId = "integrated-test"

  // Your subscription ID eg. "my-sub"
  val subscriptionId = s"$topicId-subscription"

  val topicName = ProjectTopicName.of(projectId, topicId)

  // Create a new subscription
  val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)
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
