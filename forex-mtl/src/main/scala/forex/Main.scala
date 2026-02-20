package forex

import cats.effect._
import com.github.benmanes.caffeine.cache.{ Caffeine => CaffeineBuilder }
import forex.config._
import forex.domain.Rate
import forex.services.CacheServices
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.blaze.client.BlazeClientBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      httpClient <- Stream.resource(BlazeClientBuilder[F](ec).resource)
      caffeineCache <- Stream.eval(Sync[F].delay {
                        CaffeineBuilder
                          .newBuilder()
                          .expireAfterWrite(config.cache.ttl.length, config.cache.ttl.unit)
                          .build[Rate.Pair, Rate]()
                      })
      cacheService = CacheServices.caffeine[F](caffeineCache)
      module       = new Module[F](config, cacheService, httpClient)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
