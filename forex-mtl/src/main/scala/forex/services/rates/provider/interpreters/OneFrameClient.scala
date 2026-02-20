package forex.services.rates.provider.interpreters

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.show._
import forex.domain._
import forex.services.rates.errors._
import forex.services.rates.provider.ProviderAlgebra
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.parser.decode
import org.http4s.Uri
import org.http4s.client.Client

import java.time.OffsetDateTime

class OneFrameClient[F[_]: Sync](
    oneFrameUrl: String,
    client: Client[F]
) extends ProviderAlgebra[F] {

  import OneFrameClient._

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val pairValue = s"${pair.from.show}${pair.to.show}"
    Uri.fromString(s"$oneFrameUrl/rates").map(_.withQueryParam("pair", pairValue)) match {
      case Left(_) =>
        Sync[F].pure(
          Error
            .OneFrameLookupFailed(s"Invalid one-frame URL: $oneFrameUrl")
            .asLeft[Rate]
        )

      case Right(uri) =>
        Sync[F].map(
          Sync[F]
            .attempt(client.expect[String](uri))
        ) {
          case Left(err: Throwable) =>
            Error
              .OneFrameLookupFailed(s"Request to one-frame failed: ${err.getMessage}")
              .asLeft[Rate]

          case Right(body: String) =>
            decodeAndConvert(body, pair)
              .leftMap(Error.OneFrameLookupFailed)
        }
    }
  }
}

object OneFrameClient {

  private final case class OneFrameRateResponse(
      from: String,
      to: String,
      price: BigDecimal,
      timeStamp: String
  )

  private implicit val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  private implicit val oneFrameRateDecoder: Decoder[OneFrameRateResponse] =
    deriveConfiguredDecoder[OneFrameRateResponse]

  private def decodeAndConvert(payload: String, pair: Rate.Pair): Either[String, Rate] =
    decode[List[OneFrameRateResponse]](payload)
      .leftMap(_.getMessage)
      .flatMap {
        case head :: _ =>
          toRate(head)
        case Nil =>
          Left(s"No rates returned for pair ${pair.from.show}${pair.to.show}")
      }

  private def toRate(response: OneFrameRateResponse): Either[String, Rate] =
    for {
      from <- parseCurrency(response.from)
      to <- parseCurrency(response.to)
      ts <- parseTimestamp(response.timeStamp)
    } yield
      Rate(
        pair = Rate.Pair(from, to),
        price = Price(response.price),
        timestamp = Timestamp(ts)
      )

  private def parseCurrency(value: String): Either[String, Currency] =
    Either
      .catchNonFatal(Currency.fromString(value))
      .leftMap(_ => s"Unsupported currency: $value")

  private def parseTimestamp(value: String): Either[String, OffsetDateTime] =
    Either
      .catchNonFatal(OffsetDateTime.parse(value))
      .leftMap(_ => s"Invalid timestamp: $value")
}
