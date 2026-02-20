package forex.services.rates.interpreters

import cats.Monad
import cats.implicits._
import forex.domain.Rate
import forex.services.cache.CacheAlgebra
import forex.services.provider.ProviderAlgebra
import forex.services.rates.Algebra
import forex.services.rates.errors._

class OneFrameLive[F[_]: Monad](
    cache: CacheAlgebra[F],
    provider: ProviderAlgebra[F]
) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    cache.get(pair).flatMap {
      case Some(rate) =>
        rate.asRight[Error].pure[F]

      case None =>
        provider
          .get(List(pair))
          .flatMap {
            case Right(rates) if rates.contains(pair) =>
              val rate = rates(pair)
              cache.put(pair, rate).as(rate.asRight[Error])
            case Right(_) =>
              (Error.OneFrameLookupFailed(s"No rates returned for pair ${pair.from}-${pair.to}"): Error)
                .asLeft[Rate]
                .pure[F]
            case Left(error) => error.asLeft[Rate].pure[F]
          }
    }
}
