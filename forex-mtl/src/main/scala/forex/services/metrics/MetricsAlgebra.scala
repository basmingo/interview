package forex.services.metrics

trait MetricsAlgebra[F[_]] {
  def incrementCounter(name: String, labels: Map[String, String] = Map.empty): F[Unit]
  def setGauge(name: String, value: Double, labels: Map[String, String] = Map.empty): F[Unit]
  def renderPrometheus: F[String]
}
