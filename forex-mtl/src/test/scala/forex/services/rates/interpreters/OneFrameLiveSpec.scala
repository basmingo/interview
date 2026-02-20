package forex.services.rates.interpreters

import cats.effect.concurrent.Ref
import cats.effect.IO
import forex.domain.{ Currency, Rate }
import forex.services.cache.CacheInterpreters
import forex.services.rates.errors
import forex.testkit.Fixtures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class OneFrameLiveSpec extends AnyFlatSpec with Matchers {

  private val pair        = Rate.Pair(Currency.JPY, Currency.EUR)
  private val cachedRate  = Fixtures.rate(pair, "0.91", "2026-02-20T11:00:00Z")
  private val fetchedRate = Fixtures.rate(pair, "0.92", "2026-02-20T12:00:00Z")

  "OneFrameLive" should "return cached value without calling provider" in {
    val result = for {
      cacheStore <- Ref.of[IO, Map[Rate.Pair, Rate]](Map(pair -> cachedRate))
      providerCalls <- Ref.of[IO, Int](0)
      cache    = CacheInterpreters.inMemory[IO](cacheStore)
      provider = Fixtures.countingProvider(providerCalls)(_ => Right(Map(pair -> fetchedRate)))
      service  = new OneFrameLive[IO](cache, provider)
      response <- service.get(pair)
      calls <- providerCalls.get
    } yield (response, calls)

    val (response, calls) = result.unsafeRunSync()

    response shouldBe Right(cachedRate)
    calls shouldBe 0
  }

  it should "fetch from provider on cache miss and populate cache" in {
    val result = for {
      cacheStore <- Ref.of[IO, Map[Rate.Pair, Rate]](Map.empty)
      cache    = CacheInterpreters.inMemory[IO](cacheStore)
      provider = Fixtures.provider(Right(Map(pair -> fetchedRate)))
      service  = new OneFrameLive[IO](cache, provider)
      response <- service.get(pair)
      valueInCache <- cache.get(pair)
    } yield (response, valueInCache)

    val (response, valueInCache) = result.unsafeRunSync()

    response shouldBe Right(fetchedRate)
    valueInCache shouldBe Some(fetchedRate)
  }

  it should "return a lookup error when provider does not return requested pair" in {
    val result = for {
      cacheStore <- Ref.of[IO, Map[Rate.Pair, Rate]](Map.empty)
      cache    = CacheInterpreters.inMemory[IO](cacheStore)
      provider = Fixtures.provider(Right(Map.empty))
      service  = new OneFrameLive[IO](cache, provider)
      response <- service.get(pair)
    } yield response

    result.unsafeRunSync() shouldBe Left(
      errors.Error.OneFrameLookupFailed(s"No rates returned for pair ${pair.from}-${pair.to}")
    )
  }
}
