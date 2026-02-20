package forex.services.provider.interpreters

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.show._
import cats.syntax.traverse._
import forex.domain._
import forex.services.provider.ProviderAlgebra
import forex.services.rates.errors._
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.parser.decode
import org.http4s.{ Method, Query, Request, Uri }
import org.http4s.client.Client

import java.time.OffsetDateTime

class OneFrameClient[F[_]: Sync](
    oneFrameUrl: String,
    token: String,
    client: Client[F]
) extends ProviderAlgebra[F] {

  import OneFrameClient._

  override def get(pairs: List[Rate.Pair]): F[Error Either Map[Rate.Pair, Rate]] =
    if (pairs.isEmpty) {
      Sync[F].pure(Right(Map.empty))
    } else
      buildRatesUri(pairs) match {
        case Left(_) =>
          Sync[F].pure(
            Error
              .OneFrameLookupFailed(s"Invalid one-frame URL: $oneFrameUrl")
              .asLeft[Map[Rate.Pair, Rate]]
          )

        case Right(uri) =>
          val request = Request[F](method = Method.GET, uri = uri)
            .withHeaders("token" -> token)
          Sync[F].map(
            Sync[F]
              .attempt(client.expect[String](request))
          ) {
            case Left(err: Throwable) =>
              Error
                .OneFrameLookupFailed(s"Request to one-frame failed: ${err.getMessage}")
                .asLeft[Map[Rate.Pair, Rate]]

            case Right(body: String) =>
              decodeAndConvert(body)
                .leftMap(Error.OneFrameLookupFailed)
          }
      }

  private def buildRatesUri(pairs: List[Rate.Pair]): Either[String, Uri] = {
    val pairParams = pairs.map(pair => "pair" -> s"${pair.from.show}${pair.to.show}")
    Uri
      .fromString(s"$oneFrameUrl/rates")
      .map(_.copy(query = Query.fromPairs(pairParams: _*)))
      .leftMap(_.details)
  }
}

object OneFrameClient {

  private final case class OneFrameErrorResponse(
      error: String
  )

  private final case class OneFrameRateResponse(
      from: String,
      to: String,
      price: BigDecimal,
      timeStamp: String
  )

  private implicit val circeConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  private implicit val oneFrameErrorDecoder: Decoder[OneFrameErrorResponse] =
    deriveConfiguredDecoder[OneFrameErrorResponse]
  private implicit val oneFrameRateDecoder: Decoder[OneFrameRateResponse] =
    deriveConfiguredDecoder[OneFrameRateResponse]

  private def decodeAndConvert(payload: String): Either[String, Map[Rate.Pair, Rate]] =
    decode[OneFrameErrorResponse](payload) match {
      case Right(error) =>
        Left(error.error)

      case Left(_) =>
        decode[List[OneFrameRateResponse]](payload)
          .leftMap(_.getMessage)
          .flatMap(_.traverse(toRate).map(_.map(rate => rate.pair -> rate).toMap))
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
    Currency.fromStringEither(value)

  private def parseTimestamp(value: String): Either[String, OffsetDateTime] =
    Either
      .catchNonFatal(OffsetDateTime.parse(value))
      .leftMap(_ => s"Invalid timestamp: $value")
}
