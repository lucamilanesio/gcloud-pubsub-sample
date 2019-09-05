
import java.util.concurrent.{ CompletionException, ExecutionException, TimeUnit, TimeoutException }
import java.util.function.Supplier

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

object FutureUtils {

  /**
    * Becase of the lack of support of Java to Scala Future, this converter is needed
    * @param javaFuture Java Future
    * @tparam V Future type
    */
  implicit class JavaFutureToScala[V](val javaFuture: java.util.concurrent.Future[V]) {
    import java.util.concurrent.CompletableFuture

    import scala.compat.java8.FutureConverters

    def toScala(timeout: Duration)(implicit ec: ExecutionContext): Future[V] =
      FutureConverters
        .toScala(CompletableFuture.supplyAsync(new Supplier[V] {
          override def get(): V =
            javaFuture
              .get(timeout.toSeconds, TimeUnit.SECONDS)
        }))
        .recover {
          case ce: CompletionException if ce.getCause.getClass == classOf[ExecutionException] => throw ce.getCause.getCause
          case ce: CompletionException if ce.getCause.getClass == classOf[TimeoutException]   => throw ce.getCause
          case ex: Throwable                                                                  => throw ex
        }
  }

}