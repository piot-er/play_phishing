package com.example.urlrep

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import com.example.urlrep.Model._


object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("url-rep-orchestrator")
    implicit val ec: ExecutionContext = system.dispatcher
    implicit val mat: Materializer = Materializer(system)

    val cfg = ConfigFactory.load().getConfig("urlReputation")
    val client = new UrlReputationClient(system)
    val cacheTtl = cfg.getDuration("cacheTtl").toMillis.millis
    val orchestrator = new Orchestrator(client, cacheTtl)

    val consumer = new SmsConsumer(orchestrator, system)
    consumer.run()

    println("URL Reputation Orchestrator started, listening on topic sms.filtered")
  }
}
