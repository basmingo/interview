package forex.services.cache

import forex.domain.Rate

trait CacheAlgebra[F[_]] {
  def get(key: Rate.Pair): F[Option[Rate]]
  def put(key: Rate.Pair, value: Rate): F[Unit]
  def keys: F[List[Rate.Pair]]
}
