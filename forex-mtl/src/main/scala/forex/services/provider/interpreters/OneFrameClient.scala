package forex.services.provider.interpreters

import cats.effect.Sync
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.show._
import forex.domain._
import forex.services.logging.Logger
import forex.services.metrics.MetricsAlgebra
import forex.services.provider.ProviderAlgebra
import forex.services.rates.errors._
import org.http4s.{ Method, Query, Request, Uri }
import org.http4s.client.Client

class OneFrameClient[F[_]: Sync](
    oneFrameUrl: String,
    token: String,
    client: Client[F],
    logger: Logger[F],
    metrics: MetricsAlgebra[F]
) extends ProviderAlgebra[F] {

  override def get(pairs: List[Rate.Pair]): F[Error Either Map[Rate.Pair, Rate]] =
    if (pairs.isEmpty) {
      Sync[F].pure(Right(Map.empty))
    } else {
      buildRatesUri(pairs) match {
        case Left(_)    => invalidUrl
        case Right(uri) => fetch(uri)
      }
    }

  private def fetch(uri: Uri): F[Error Either Map[Rate.Pair, Rate]] = {
    val request = Request[F](method = Method.GET, uri = uri)
      .withHeaders("token" -> token)
    for {
      attempted <- Sync[F].attempt(client.expect[String](request))
      result <- attempted match {
                 case Left(err) => requestFailed(err)
                 case Right(body) =>
                   Sync[F].pure(
                     OneFramePayloadDecoder
                       .parse(body)
                       .leftMap(msg => (Error.OneFrameLookupFailed(msg): Error))
                   )
               }
      _ <- logAndMeasure(result)
    } yield result
  }

  private def invalidUrl: F[Error Either Map[Rate.Pair, Rate]] =
    for {
      _ <- logger.warn(s"One-frame URL is invalid: $oneFrameUrl")
      _ <- metrics.incrementCounter("provider_requests_total", Map("result" -> "error", "reason" -> "invalid_url"))
    } yield
      (Error.OneFrameLookupFailed(s"Invalid one-frame URL: $oneFrameUrl"): Error)
        .asLeft[Map[Rate.Pair, Rate]]

  private def requestFailed(err: Throwable): F[Error Either Map[Rate.Pair, Rate]] =
    Sync[F].pure(
      (Error.OneFrameLookupFailed(s"Request to one-frame failed: ${err.getMessage}"): Error)
        .asLeft[Map[Rate.Pair, Rate]]
    )

  private def logAndMeasure(result: Error Either Map[Rate.Pair, Rate]): F[Unit] =
    result match {
      case Left(Error.OneFrameLookupFailed(msg)) =>
        for {
          _ <- logger.warn(s"One-frame lookup failed: $msg")
          _ <- metrics.incrementCounter("provider_requests_total", Map("result" -> "error"))
        } yield ()

      case Right(rates) =>
        for {
          _ <- metrics.incrementCounter("provider_requests_total", Map("result" -> "success"))
          _ <- metrics.setGauge("provider_rates_last_batch_size", rates.size.toDouble)
        } yield ()
    }

  private def buildRatesUri(pairs: List[Rate.Pair]): Either[String, Uri] = {
    val pairParams = pairs.map(pair => "pair" -> s"${pair.from.show}${pair.to.show}")
    Uri
      .fromString(s"$oneFrameUrl/rates")
      .map(_.copy(query = Query.fromPairs(pairParams: _*)))
      .leftMap(_.details)
  }
}
