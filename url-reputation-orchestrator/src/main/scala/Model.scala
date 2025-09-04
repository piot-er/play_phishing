package com.example.urlrep

import io.circe._
import io.circe.generic.semiauto._

object Model {

  sealed trait ThreatType
  object ThreatType {
    case object SOCIAL_ENGINEERING extends ThreatType
    case object MALWARE extends ThreatType
    case object UNWANTED_SOFTWARE extends ThreatType

    val all = List(SOCIAL_ENGINEERING, MALWARE, UNWANTED_SOFTWARE)

    implicit val encoder: io.circe.Encoder[ThreatType] = io.circe.Encoder.encodeString.contramap {
      case SOCIAL_ENGINEERING => "SOCIAL_ENGINEERING"
      case MALWARE => "MALWARE"
      case UNWANTED_SOFTWARE => "UNWANTED_SOFTWARE"
    }

    implicit val decoder: io.circe.Decoder[ThreatType] = io.circe.Decoder.decodeString.emap {
      case "SOCIAL_ENGINEERING" => Right(SOCIAL_ENGINEERING)
      case "MALWARE" => Right(MALWARE)
      case "UNWANTED_SOFTWARE" => Right(UNWANTED_SOFTWARE)
      case other => Left(s"Unknown ThreatType: $other")
    }
  }

  sealed trait ConfidenceLevel
  object ConfidenceLevel {
    case object SAFE extends ConfidenceLevel
    case object LOW extends ConfidenceLevel
    case object MEDIUM extends ConfidenceLevel
    case object HIGH extends ConfidenceLevel
    case object HIGHER extends ConfidenceLevel
    case object VERY_HIGH extends ConfidenceLevel
    case object EXTREMELY_HIGH extends ConfidenceLevel

    implicit val encoder: io.circe.Encoder[ConfidenceLevel] = io.circe.Encoder.encodeString.contramap {
      case SAFE => "SAFE"
      case LOW => "LOW"
      case MEDIUM => "MEDIUM"
      case HIGH => "HIGH"
      case HIGHER => "HIGHER"
      case VERY_HIGH => "VERY_HIGH"
      case EXTREMELY_HIGH => "EXTREMELY_HIGH"
    }

    implicit val decoder: io.circe.Decoder[ConfidenceLevel] = io.circe.Decoder.decodeString.emap {
      case "SAFE" => Right(SAFE)
      case "LOW" => Right(LOW)
      case "MEDIUM" => Right(MEDIUM)
      case "HIGH" => Right(HIGH)
      case "HIGHER" => Right(HIGHER)
      case "VERY_HIGH" => Right(VERY_HIGH)
      case "EXTREMELY_HIGH" => Right(EXTREMELY_HIGH)
      case other => Left(s"Unknown ConfidenceLevel: $other")
    }
  }


  final case class VerifyRequest(uri: String, threatTypes: List[ThreatType], allowScan: Boolean = true)
  object VerifyRequest {
    implicit val enc: Encoder[VerifyRequest] = deriveEncoder[VerifyRequest]
  }

  final case class Score(threatType: ThreatType, confidenceLevel: ConfidenceLevel)
  object Score {
    implicit val dec: Decoder[Score] = deriveDecoder[Score]
  }

  final case class VerifyResponse(scores: List[Score])
  object VerifyResponse {
    implicit val dec: Decoder[VerifyResponse] = deriveDecoder[VerifyResponse]
  }

  final case class AggregatedResult(uri: String, perType: Map[ThreatType, ConfidenceLevel])
}
