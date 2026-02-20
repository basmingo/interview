package forex.programs.rates

import cats.effect.IO
import forex.domain.{ Currency, Rate }
import forex.testkit.Fixtures
import forex.services.rates.errors
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProgramSpec extends AnyFlatSpec with Matchers {

  private val pair    = Rate.Pair(Currency.USD, Currency.EUR)
  private val request = Protocol.GetRatesRequest(pair.from, pair.to)

  "Program" should "return rate on successful service lookup" in {
    val expectedRate = Fixtures.rate(pair, "1.2345", "2026-02-20T10:00:00Z")
    val service      = Fixtures.ratesService(Right(expectedRate))
    val program      = Program[IO](service)

    program.get(request).unsafeRunSync() shouldBe Right(expectedRate)
  }

  it should "map service error into program error" in {
    val serviceError = errors.Error.OneFrameLookupFailed("provider unavailable")
    val service      = Fixtures.ratesService(Left(serviceError))
    val program      = Program[IO](service)

    program.get(request).unsafeRunSync() shouldBe Left(
      forex.programs.rates.errors.Error.RateLookupFailed("provider unavailable")
    )
  }
}
