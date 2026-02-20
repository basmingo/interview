package forex.services.cache.interpreters

import cats.effect.Sync
import com.github.benmanes.caffeine.cache.{ Cache => CaffeineCache }
import forex.domain.Rate
import forex.services.cache.CacheAlgebra
import scala.jdk.CollectionConverters._

class Caffeine[F[_]: Sync](
    cache: CaffeineCache[Rate.Pair, Rate]
) extends CacheAlgebra[F] {

  override def get(key: Rate.Pair): F[Option[Rate]] =
    Sync[F].delay(Option(cache.getIfPresent(key)))

  override def put(key: Rate.Pair, value: Rate): F[Unit] =
    Sync[F].delay(cache.put(key, value))

  override def keys: F[List[Rate.Pair]] =
    Sync[F].delay(cache.asMap().keySet().asScala.toList)
}
