package forex.http
package rates

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import forex.programs.rates.errors.Error
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

import java.util.UUID

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromRaw) +& ToQueryParam(toRaw) =>
      validateRequest(fromRaw, toRaw) match {
        case Left(err) =>
          BadRequest(err)
        case Right(request) =>
          rates.get(request).flatMap {
            case Right(rate) =>
              Ok(rate.asGetApiResponse)
            case Left(Error.RateLookupFailed(msg)) =>
              upstreamError("RATE_LOOKUP_FAILED", msg)
          }
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

  private def validateRequest(
      fromRaw: Option[String],
      toRaw: Option[String]
  ): Either[Protocol.ApiErrorResponse, RatesProgramProtocol.GetRatesRequest] =
    (fromRaw, toRaw) match {
      case (None, _) =>
        Left(invalidRequest("MISSING_QUERY_PARAM", "Missing required query parameter: from"))
      case (_, None) =>
        Left(invalidRequest("MISSING_QUERY_PARAM", "Missing required query parameter: to"))
      case (Some(from), Some(to)) =>
        for {
          parsedFrom <- parseCurrency(from).leftMap(msg => invalidRequest("UNSUPPORTED_CURRENCY", msg))
          parsedTo <- parseCurrency(to).leftMap(msg => invalidRequest("UNSUPPORTED_CURRENCY", msg))
        } yield RatesProgramProtocol.GetRatesRequest(parsedFrom, parsedTo)
    }

  private def invalidRequest(code: String, message: String): Protocol.ApiErrorResponse =
    buildError(
      errorType = "INVALID_REQUEST",
      errorCode = code,
      errorMessage = message,
      displayMessage = "The selected currency pair is invalid. Please choose supported currencies."
    )

  private def upstreamError(code: String, message: String): F[org.http4s.Response[F]] =
    BadGateway(
      buildError(
        errorType = "UPSTREAM_ERROR",
        errorCode = code,
        errorMessage = message,
        displayMessage = "Unable to fetch rates from provider. Please try again later."
      )
    )

  private def buildError(
      errorType: String,
      errorCode: String,
      errorMessage: String,
      displayMessage: String
  ): Protocol.ApiErrorResponse =
    Protocol.ApiErrorResponse(
      errorType = errorType,
      errorCode = errorCode,
      errorMessage = errorMessage,
      displayMessage = displayMessage,
      requestId = s"req_${UUID.randomUUID().toString.replace("-", "").take(12)}"
    )

}
