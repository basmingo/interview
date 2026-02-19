package forex

package object services {
  type RatesService[F[_]] = rates.Algebra[F]
  type CacheService[F[_]] = cache.CacheAlgebra[F]

  final val RatesServices = rates.Interpreters
  final val CacheServices = cache.CacheInterpreters
}
