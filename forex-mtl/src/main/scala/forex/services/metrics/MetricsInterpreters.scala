package forex.services.metrics

import cats.Applicative
import cats.effect.Sync
import cats.effect.concurrent.Ref
import forex.services.metrics.interpreters.{InMemoryMetrics, NoOpMetrics}

object MetricsInterpreters {

  def noop[F[_]: Applicative]: MetricsAlgebra[F] =
    new NoOpMetrics[F]

  def inMemory[F[_]: Sync](ref: Ref[F, MetricsState]): MetricsAlgebra[F] =
    new InMemoryMetrics[F](ref)
}
