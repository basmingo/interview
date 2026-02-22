package forex.services.logging

import cats.Applicative
import cats.effect.Sync
import org.slf4j.{ LoggerFactory, Logger => Slf4j }

object LoggingInterpreters {

  def slf4j[F[_]: Sync](name: String): Logger[F] =
    new Slf4jLogger[F](LoggerFactory.getLogger(name))

  def noop[F[_]: Applicative]: Logger[F] =
    new NoOpLogger[F]

  private final class Slf4jLogger[F[_]: Sync](underlying: Slf4j) extends Logger[F] {
    override def info(message: => String): F[Unit]  = Sync[F].delay(underlying.info(message))
    override def warn(message: => String): F[Unit]  = Sync[F].delay(underlying.warn(message))
    override def error(message: => String): F[Unit] = Sync[F].delay(underlying.error(message))
  }

  private final class NoOpLogger[F[_]: Applicative] extends Logger[F] {
    override def info(message: => String): F[Unit]  = Applicative[F].unit
    override def warn(message: => String): F[Unit]  = Applicative[F].unit
    override def error(message: => String): F[Unit] = Applicative[F].unit
  }
}
