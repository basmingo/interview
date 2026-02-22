package forex.services.rates

import cats.{ Applicative, Monad }
import cats.effect.Sync
import forex.services.cache.CacheAlgebra
import forex.services.logging.Logger
import forex.services.metrics.MetricsAlgebra
import forex.services.provider.ProviderAlgebra
import forex.services.provider.interpreters.OneFrameClient
import interpreters._
import org.http4s.client.Client

object Interpreters {

  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def oneFrame[F[_]: Sync](
      url: String,
      token: String,
      client: Client[F],
      logger: Logger[F],
      metrics: MetricsAlgebra[F]
  ): ProviderAlgebra[F] =
    new OneFrameClient[F](url, token, client, logger, metrics)

  def live[F[_]: Monad](cache: CacheAlgebra[F], ratesProvider: ProviderAlgebra[F]): Algebra[F] =
    new OneFrameLive[F](cache, ratesProvider)

}
