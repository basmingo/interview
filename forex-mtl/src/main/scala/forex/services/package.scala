package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  type CacheService[F[_]] = cache.CacheAlgebra[F]
  type AppLogger[F[_]]    = logging.Logger[F]
  type AppMetrics[F[_]]   = metrics.MetricsAlgebra[F]

  final val RatesServices   = rates.Interpreters
  final val CacheServices   = cache.CacheInterpreters
  final val LoggingServices = logging.LoggingInterpreters
  final val MetricsServices = metrics.MetricsInterpreters
}
