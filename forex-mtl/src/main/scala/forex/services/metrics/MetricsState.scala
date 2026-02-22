package forex.services.metrics

final case class MetricsState(
    counters: Map[MetricKey, Double],
    gauges: Map[MetricKey, Double]
)

object MetricsState {
  val empty: MetricsState = MetricsState(counters = Map.empty, gauges = Map.empty)
}
