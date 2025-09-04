package com.example.smsingest
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import io.circe.parser.decode
import io.circe.generic.auto._
import java.io.{BufferedReader, FileReader}
import java.util.Properties

object SmsIngestApp extends App {
  val bootstrapServers = sys.env.getOrElse("BOOTSTRAP_SERVERS", "kafka:9092")
  val topic = sys.env.getOrElse("SMS_INPUT_TOPIC", "sms.raw")

  val props = new Properties()
  props.put("bootstrap.servers", bootstrapServers)
  props.put("key.serializer", classOf[StringSerializer].getName)
  props.put("value.serializer", classOf[StringSerializer].getName)

  val producer = new KafkaProducer[String, String](props)

  val reader = new BufferedReader(new FileReader("/app/data/sms.jsonl"))
  var line: String = reader.readLine()
  while (line != null) {
    decode[SmsMessage](line) match {
      case Right(sms) =>
        val value = s"""{"sender":"${sms.sender}","receiver":"${sms.receiver}","msg":"${sms.msg}"}"""
        val record = new ProducerRecord[String, String](topic, sms.receiver, value)
        producer.send(record, (_, _) => println(value))
      case Left(_) => // skip invalid lines
    }
    line = reader.readLine()
  }
  reader.close()
  producer.close()
  println("Finished ingest.")
}
