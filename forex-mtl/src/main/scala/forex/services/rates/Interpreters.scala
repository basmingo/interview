package forex.services.rates

import cats.{ Applicative, Monad }
import forex.services.cache.CacheAlgebra
import forex.services.rates.provider.{Algebra => RatesProvider}
import forex.services.rates.provider.interpreters.OneFrameStub
import interpreters._

object Interpreters {

  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def oneFrameStub[F[_]: Applicative](url: String): provider.Algebra[F] =
    new OneFrameStub[F](url)

  def live[F[_]: Monad](cache: CacheAlgebra[F], ratesProvider: RatesProvider[F]): Algebra[F] =
    new OneFrameLive[F](cache, ratesProvider)

}
