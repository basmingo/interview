package forex.services.cache

import cats.Functor
import cats.effect.Sync
import cats.effect.concurrent.Ref
import com.github.benmanes.caffeine.cache.{ Cache => CaffeineCache }
import forex.domain.Rate
import forex.services.cache.interpreters.{ Caffeine, InMemoryCache }

object CacheInterpreters {

  def inMemory[F[_]: Functor](store: Ref[F, Map[Rate.Pair, Rate]]): CacheAlgebra[F] = new InMemoryCache[F](store)

  def caffeine[F[_]: Sync](cache: CaffeineCache[Rate.Pair, Rate]): CacheAlgebra[F] = new Caffeine[F](cache)

}
