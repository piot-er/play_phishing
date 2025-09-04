package com.example.consentgate

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.scaladsl.{Consumer, Producer}
import org.apache.pekko.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import org.apache.pekko.stream.scaladsl.{Sink}
import org.apache.pekko.stream.Materializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

import scala.concurrent.ExecutionContext
import scala.collection.concurrent.TrieMap

// JSON model
final case class Sms(sender: String, receiver: String, msg: String)
object Sms {
  implicit val decoder: Decoder[Sms] = deriveDecoder[Sms]
  implicit val encoder: Encoder[Sms] = deriveEncoder[Sms]
}
case class ConsentState(receiver: String, optIn: String)
object ConsentState {
  implicit val decoder: Decoder[ConsentState] = deriveDecoder
}

object ConsentGateApp extends App {
  implicit val system: ActorSystem = ActorSystem("consent-gate")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mat: Materializer = Materializer(system)

  val bootstrap = sys.env.getOrElse("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")

  val consumerSettings =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrap)
      .withGroupId("consent-gate-sms")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  val producerSettings =
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrap)

  val consentCache = TrieMap.empty[String, String]

  val consentConsumerSettings =
    ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrap)
      .withGroupId("consent-gate-consent")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  Consumer
    .plainSource(consentConsumerSettings, Subscriptions.topics("consent.state"))
    .map { record =>
      val receiverKey = record.key()
      val status = decode[ConsentState](record.value()) match {
        case Right(consent) if consent.optIn.toLowerCase == "true" =>
          "OPT_IN"
        case Right(_) =>
          "OPT_OUT"
        case Left(error) =>
          println(s"JSON parse error: $error, raw=${record.value()}")
          ""
      }
      consentCache.put(receiverKey, status)
      println(s"Consent updated: $receiverKey -> $status")
    }
    .to(Sink.ignore)
    .run()


  Consumer
    .plainSource(consumerSettings, Subscriptions.topics("sms.raw"))
    .mapConcat { record =>
      decode[Sms](record.value()).toOption.toList.flatMap { sms =>
        consentCache.get(sms.receiver) match {
          case Some("OPT_IN") =>
            println(s"Forwarding SMS to ${sms.receiver}")
            List(new ProducerRecord[String, String]("sms.filtered", sms.receiver, record.value()))
          case other =>
            println(s"Dropping SMS for ${sms.receiver}, consent=$other")
            Nil
        }
      }
    }
    .runWith(Producer.plainSink(producerSettings))

  println("ConsentGateApp started, consuming from sms.raw and consent.state")
}
