package forex.services.cache

import cats.effect._
import cats.implicits._
import forex.services.rates.Algebra
import fs2.Stream

import scala.concurrent.duration._

class CacheUpdater[F[_]: Timer: ConcurrentEffect](
    ratesService: Algebra[F],
    cache: CacheAlgebra[F]
) {

  def stream: Stream[F, Unit] =
    Stream.fixedRate(4.minutes).evalMap { _ =>
      for {
        keys <- cache.keys
        _ <- keys.traverse(pair => ratesService.get(pair))
      } yield ()
    }
}
