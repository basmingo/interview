package forex.services.metrics.interpreters

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import forex.services.metrics.MetricsAlgebra

class NoOpMetrics[F[_]: Applicative] extends MetricsAlgebra[F] {
  override def incrementCounter(name: String, labels: Map[String, String]): F[Unit]        = ().pure[F]
  override def setGauge(name: String, value: Double, labels: Map[String, String]): F[Unit] = ().pure[F]
  override def renderPrometheus: F[String]                                                 = "".pure[F]
}
