package forex.services.cache

import cats.effect.IO
import cats.effect.Timer
import cats.effect.concurrent.Ref
import com.github.benmanes.caffeine.cache.{ Caffeine => CaffeineBuilder }
import forex.domain.{ Currency, Rate }
import forex.services.logging.LoggingInterpreters
import forex.services.metrics.MetricsInterpreters
import forex.services.rates.interpreters.OneFrameLive
import forex.testkit.Fixtures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext

class CacheWarmupIntegrationSpec extends AnyFlatSpec with Matchers {

  private val pair1                     = Rate.Pair(Currency.USD, Currency.EUR)
  private val pair2                     = Rate.Pair(Currency.JPY, Currency.GBP)
  private implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  "CacheUpdater" should "warm cache and allow OneFrameLive to serve cache hits" in {
    val result = for {
      providerCalls <- Ref.of[IO, Int](0)
      caffeine <- IO {
                   CaffeineBuilder
                     .newBuilder()
                     .expireAfterWrite(5, TimeUnit.MINUTES)
                     .build[Rate.Pair, Rate]()
                 }
      cache = CacheInterpreters.caffeine[IO](caffeine)
      provider = Fixtures.countingProvider(providerCalls)(
        pairs => Right(pairs.map(p => p -> Fixtures.deterministicRate(p)).toMap)
      )
      updater =
        new CacheRefreshJob[IO](provider, cache, LoggingInterpreters.noop[IO], MetricsInterpreters.noop[IO])
      _ <- updater.stream.take(1).compile.drain
      callsAfterWarmup <- providerCalls.get
      live = new OneFrameLive[IO](cache, provider)
      response1 <- live.get(pair1)
      response2 <- live.get(pair2)
      callsAfterReads <- providerCalls.get
      keys <- cache.keys
    } yield (callsAfterWarmup, callsAfterReads, response1, response2, keys)

    val (callsAfterWarmup, callsAfterReads, response1, response2, keys) = result.unsafeRunSync()

    callsAfterWarmup should be > 0
    callsAfterReads shouldBe callsAfterWarmup
    response1 shouldBe Right(Fixtures.deterministicRate(pair1))
    response2 shouldBe Right(Fixtures.deterministicRate(pair2))
    keys should contain(pair1)
    keys should contain(pair2)
  }
}
