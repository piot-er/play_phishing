package com.example.urlrep

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.scaladsl.{Consumer, Producer}
import org.apache.pekko.stream.scaladsl.{Sink, Keep}
import org.apache.pekko.kafka.{ConsumerSettings, Subscriptions}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.Materializer
import io.circe.parser.decode
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.matching.Regex
import com.example.urlrep.Model._
import org.apache.pekko.kafka.ProducerSettings
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.pekko.kafka.scaladsl.Producer
import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.circe.syntax._
import io.circe.generic.auto._

final case class Sms(sender: String, receiver: String, msg: String)
object Sms {
  implicit val dec: io.circe.Decoder[Sms] = io.circe.generic.semiauto.deriveDecoder[Sms]
  implicit val smsEncoder: io.circe.Encoder[Sms] = io.circe.generic.semiauto.deriveEncoder[Sms]
}

final case class SmsProcessed(originalSms: Sms, result: Map[ThreatType, ConfidenceLevel])
object SmsProcessed {
  implicit val encoder: Encoder[SmsProcessed] = new Encoder[SmsProcessed] {
    final def apply(sp: SmsProcessed): Json = {
      // Zamiana mapy ThreatType -> ConfidenceLevel na obiekt JSON
      val resultJson = Json.obj(
        sp.result.map { case (threat, confidence) =>
          threat.toString -> Json.fromString(confidence.toString)
        }.toSeq: _*
      )

      Json.obj(
        "originalSms" -> Json.obj(
          "sender" -> Json.fromString(sp.originalSms.sender),
          "receiver" -> Json.fromString(sp.originalSms.receiver),
          "msg" -> Json.fromString(sp.originalSms.msg)
        ),
        "result" -> resultJson
      )
    }
  }
}

class SmsConsumer(orchestrator: Orchestrator, system: ActorSystem)(implicit mat: Materializer, ec: ExecutionContext) {

  private val consumerSettings =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("kafka:9092")
      .withGroupId("url-rep-orchestrator")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("kafka:9092")

  private val urlRegex: Regex =
    """https?://[^\s]+""".r

  def run(): Unit = {
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics("sms.filtered"))
      .map { record =>
        decode[Sms](record.value()).toOption
      }
      .collect { case Some(sms) => sms }
      .mapAsync(4) { sms =>
        val urls = urlRegex.findAllIn(sms.msg).toList
        if (urls.nonEmpty) {
          Future.sequence(urls.map { url =>
            orchestrator.check(url, ThreatType.all).map { res =>
              (url, res.perType)
            }
          }).map { results =>
            val mergedResult = results.flatMap(_._2).toMap
            val processed = SmsProcessed(sms, mergedResult)
            val json = processed.asJson.noSpaces
            new ProducerRecord[String, String]("sms.processed", json)
          }
        } else {
          val processed = SmsProcessed(sms, Map.empty)
          val json = processed.asJson.noSpaces
          Future.successful(new ProducerRecord[String, String]("sms.processed", json))
        }
      }
      .toMat(Producer.plainSink(producerSettings))(Keep.right)
      .run()
  }
}
