package forex.services.cache.interpreters

import cats.Functor
import cats.syntax.functor._
import cats.effect.concurrent.Ref
import forex.domain.Rate
import forex.services.cache.CacheAlgebra

class InMemoryCache[F[_]: Functor](
    store: Ref[F, Map[Rate.Pair, Rate]]
) extends CacheAlgebra[F] {

  override def get(key: Rate.Pair): F[Option[Rate]] =
    store.get.map(_.get(key))

  override def put(key: Rate.Pair, value: Rate): F[Unit] =
    store.update(_.updated(key, value))

  override def keys: F[List[Rate.Pair]] =
    store.get.map(_.keys.toList)
}
