package forex.services.provider.interpreters

import cats.syntax.either._
import cats.syntax.traverse._
import forex.domain._
import forex.services.provider.ProviderProtocol.{ OneFrameErrorResponse, OneFrameRateResponse }
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

import java.time.OffsetDateTime

object OneFramePayloadDecoder {

  private implicit val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  private implicit val oneFrameErrorDecoder: Decoder[OneFrameErrorResponse] =
    deriveConfiguredDecoder[OneFrameErrorResponse]

  private implicit val oneFrameRateDecoder: Decoder[OneFrameRateResponse] =
    deriveConfiguredDecoder[OneFrameRateResponse]

  def parse(payload: String): Either[String, Map[Rate.Pair, Rate]] =
    io.circe.parser.decode[OneFrameErrorResponse](payload) match {
      case Right(error) =>
        Left(error.error)

      case Left(_) =>
        io.circe.parser
          .decode[List[OneFrameRateResponse]](payload)
          .leftMap(_.getMessage)
          .flatMap(_.traverse(toRate).map(_.map(rate => rate.pair -> rate).toMap))
    }

  private def toRate(response: OneFrameRateResponse): Either[String, Rate] =
    for {
      from <- Currency.fromStringEither(response.from)
      to <- Currency.fromStringEither(response.to)
      ts <- parseTimestamp(response.timeStamp)
    } yield
      Rate(
        pair = Rate.Pair(from, to),
        price = Price(response.price),
        timestamp = Timestamp(ts)
      )

  private def parseTimestamp(value: String): Either[String, OffsetDateTime] =
    Either
      .catchNonFatal(OffsetDateTime.parse(value))
      .leftMap(_ => s"Invalid timestamp: $value")
}
