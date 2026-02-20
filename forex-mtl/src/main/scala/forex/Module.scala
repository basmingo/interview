package forex

import cats.effect.{Concurrent, Timer}
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services.cache.CacheRefreshJob
import forex.services._
import fs2.Stream
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, cache: CacheService[F], client: Client[F]) {

  private val ratesProvider = RatesServices.oneFrame[F](config.oneFrame.url, config.oneFrame.token, client)

  private val ratesService: RatesService[F] = RatesServices.live[F](cache, ratesProvider)
  private val cacheUpdater: CacheRefreshJob[F] = new CacheRefreshJob[F](ratesProvider, cache)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = http => AutoSlash(http)

  private val appMiddleware: TotalMiddleware = http => Timeout(config.http.timeout)(http)

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)
  val cacheUpdaterStream: Stream[F, Unit] = cacheUpdater.stream

}
