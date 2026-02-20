package forex.services.cache

import cats.effect._
import cats.implicits._
import forex.domain.{ Currency, Rate }
import forex.services.provider.ProviderAlgebra
import fs2.Stream

import scala.concurrent.duration._

class CacheRefreshJob[F[_]: Timer: Sync](
    provider: ProviderAlgebra[F],
    cache: CacheAlgebra[F]
) {

  private val refreshInterval = 4.minutes

  private val allPairs: List[Rate.Pair] = Rate.allPairs(Currency.values)

  private def refreshBatch(pairs: List[Rate.Pair]): F[Unit] =
    provider.get(pairs).flatMap {
      case Right(rates) =>
        rates.values.toList.traverse_(rate => cache.put(rate.pair, rate))
      case Left(_) =>
        Sync[F].unit
    }

  def stream: Stream[F, Unit] =
    Stream.eval(refreshBatch(allPairs)) ++ Stream.fixedRate(refreshInterval).evalMap(_ => refreshBatch(allPairs))
}
