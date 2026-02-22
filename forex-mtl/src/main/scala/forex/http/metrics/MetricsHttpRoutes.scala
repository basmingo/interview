package forex.http.metrics

import cats.effect.Sync
import cats.syntax.flatMap._
import forex.services.AppMetrics
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class MetricsHttpRoutes[F[_]: Sync](metrics: AppMetrics[F]) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root =>
      metrics.renderPrometheus.flatMap(Ok(_))
  }

  val routes: HttpRoutes[F] = Router(
    "/metrics" -> httpRoutes
  )
}
