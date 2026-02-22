package forex.services.logging

trait Logger[F[_]] {
  def info(message: => String): F[Unit]
  def warn(message: => String): F[Unit]
  def error(message: => String): F[Unit]
}
