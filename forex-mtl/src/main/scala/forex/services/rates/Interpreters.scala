package forex.services.rates

import cats.{ Applicative, Monad }
import cats.effect.Sync
import forex.services.cache.CacheAlgebra
import forex.services.rates.provider.{ProviderAlgebra => RatesProvider}
import forex.services.rates.provider.interpreters.OneFrameClient
import interpreters._
import org.http4s.client.Client

object Interpreters {

  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def oneFrameStub[F[_]: Sync](url: String, client: Client[F]): provider.ProviderAlgebra[F] =
    new OneFrameClient[F](url, client)

  def live[F[_]: Monad](cache: CacheAlgebra[F], ratesProvider: RatesProvider[F]): Algebra[F] =
    new OneFrameLive[F](cache, ratesProvider)

}
