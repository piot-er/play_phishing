package com.example.urlrep

import com.example.urlrep.Model._
import com.github.benmanes.caffeine.cache.{Caffeine, Cache}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

final class Orchestrator(client: UrlReputationClient, cacheTtl: FiniteDuration)(implicit ec: ExecutionContext) {

  private val cache: Cache[String, AggregatedResult] =
    Caffeine.newBuilder()
      .expireAfterWrite(cacheTtl.toMillis, TimeUnit.MILLISECONDS)
      .maximumSize(100_000L)
      .build[String, AggregatedResult]()

  private def makeKey(uri: String, tps: List[ThreatType]) =
    s"$uri|" + tps.map(_.toString).sorted.mkString(",")

  def check(uri: String, threatTypes: List[ThreatType]): Future[AggregatedResult] = {
    val key = makeKey(uri, threatTypes)
    Option(cache.getIfPresent(key)) match {
      case Some(hit) => Future.successful(hit)
      case None =>
        client.verify(uri, threatTypes).map { resp =>
          val perType = resp.scores.map(s => s.threatType -> s.confidenceLevel).toMap
          val result  = AggregatedResult(uri, perType)
          cache.put(key, result)
          result
        }
    }
  }
}
