package com.example.urlrep

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.stream.Materializer
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import io.circe.syntax._
import io.circe.parser._
import com.typesafe.config.ConfigFactory
import org.apache.pekko.pattern.CircuitBreaker

import com.example.urlrep.Model._

final class UrlReputationClient(system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) {

  private val cfg  = ConfigFactory.load().getConfig("urlReputation")
  private val base = cfg.getString("baseUrl").stripSuffix("/")
  private val path = cfg.getString("path")
  private val apiKey = cfg.getString("apiKey")
  private val reqTimeout = cfg.getDuration("requestTimeout").toMillis.millis

  private val breaker = {
    val b = cfg.getConfig("breaker")
    new CircuitBreaker(
      scheduler = system.scheduler,
      maxFailures = b.getInt("maxFailures"),
      callTimeout = b.getDuration("callTimeout").toMillis.millis,
      resetTimeout = b.getDuration("resetTimeout").toMillis.millis
    )(system.dispatcher)
  }

  private def authHeader: List[HttpHeader] =
    if (apiKey.nonEmpty) List(RawHeader("Authorization", apiKey)) else Nil

  def verify(uri: String, types: List[ThreatType]): Future[VerifyResponse] = {
    val payload = VerifyRequest(uri, types, allowScan = true).asJson.noSpaces
    val httpReq = HttpRequest(
      method = HttpMethods.POST,
      uri    = s"$base$path",
      headers = authHeader,
      entity = HttpEntity(ContentTypes.`application/json`, payload)
    )

    val call = Http()(system)
      .singleRequest(httpReq)
      .flatMap { resp =>
        if (resp.status.isSuccess()) Unmarshal(resp.entity).to[String]
        else Unmarshal(resp.entity).to[String].flatMap(body =>
          Future.failed(new RuntimeException(s"Verifier HTTP ${resp.status.intValue()} body=$body"))
        )
      }
      .map { body =>
        decode[VerifyResponse](body).fold(
          err => throw new RuntimeException(s"JSON parse error: ${err.getMessage} body=$body"),
          identity
        )
      }

    val withBreaker = breaker.withCircuitBreaker(Future.firstCompletedOf(Seq(call, after(reqTimeout)(Future.failed(new java.util.concurrent.TimeoutException)))))
    retry(2)(withBreaker)
  }

  private def after[T](d: FiniteDuration)(f: => Future[T]): Future[T] = {
    val p = scala.concurrent.Promise[T]()
    system.scheduler.scheduleOnce(d) { p.completeWith(f) }(system.dispatcher)
    p.future
  }

  private def retry[T](n: Int)(f: => Future[T]): Future[T] =
    f.recoverWith { case _ if n > 0 => retry(n - 1)(f) }
}
