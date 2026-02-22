package forex.services.metrics.interpreters

import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.functor._
import forex.services.metrics.{ MetricKey, MetricsAlgebra, MetricsState }

class InMemoryMetrics[F[_]: Sync](ref: Ref[F, MetricsState]) extends MetricsAlgebra[F] {

  override def incrementCounter(name: String, labels: Map[String, String]): F[Unit] = {
    val key = MetricKey(name, labels)
    ref.update { state =>
      val current = state.counters.getOrElse(key, 0d)
      state.copy(counters = state.counters.updated(key, current + 1d))
    }
  }

  override def setGauge(name: String, value: Double, labels: Map[String, String]): F[Unit] = {
    val key = MetricKey(name, labels)
    ref.update(state => state.copy(gauges = state.gauges.updated(key, value)))
  }

  override def renderPrometheus: F[String] =
    ref.get.map { state =>
      val counters = state.counters.toList
        .sortBy(_._1.rendered)
        .map { case (k, v) => s"${k.rendered} $v" }

      val gauges = state.gauges.toList
        .sortBy(_._1.rendered)
        .map { case (k, v) => s"${k.rendered} $v" }
      (counters ++ gauges)
        .mkString("", "\n", if (counters.isEmpty && gauges.isEmpty) "" else "\n")
    }
}
