package forex.services.rates.interpreters

import cats.Monad
import cats.implicits._
import forex.domain.Rate
import forex.services.cache.CacheAlgebra
import forex.services.rates.Algebra
import forex.services.rates.errors._
import forex.services.rates.provider.{ ProviderAlgebra => ProviderAlgebra }

class OneFrameLive[F[_]: Monad](
    cache: CacheAlgebra[F],
    provider: ProviderAlgebra[F]
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    cache.get(pair).flatMap {
      case Some(rate) =>
        (rate.asRight[Error]).pure[F]

      case None =>
        provider.get(pair).flatMap {
          case Right(rate) => cache.put(pair, rate).as(rate.asRight[Error])
          case Left(error) => error.asLeft[Rate].pure[F]
        }
    }
}
