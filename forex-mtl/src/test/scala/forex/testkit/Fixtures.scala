package forex.testkit

import cats.effect.IO
import cats.effect.concurrent.Ref
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.provider.ProviderAlgebra
import forex.services.rates
import forex.services.rates.errors

import java.time.OffsetDateTime

object Fixtures {

  def timestamp(iso: String): Timestamp =
    Timestamp(OffsetDateTime.parse(iso))

  def rate(pair: Rate.Pair, price: String, at: String): Rate =
    Rate(pair, Price(BigDecimal(price)), timestamp(at))

  def deterministicRate(pair: Rate.Pair): Rate = {
    val normalized = (pair.from.hashCode.abs + pair.to.hashCode.abs) % 1000
    Rate(
      pair = pair,
      price = Price(BigDecimal(normalized) / 100),
      timestamp = timestamp("2026-02-21T00:00:00Z")
    )
  }

  def ratesService(result: errors.Error Either Rate): rates.Algebra[IO] =
    (_: Rate.Pair) => IO.pure(result)

  def provider(result: errors.Error Either Map[Rate.Pair, Rate]): ProviderAlgebra[IO] =
    (_: List[Rate.Pair]) => IO.pure(result)

  def countingProvider(
      counter: Ref[IO, Int]
  )(f: List[Rate.Pair] => errors.Error Either Map[Rate.Pair, Rate]): ProviderAlgebra[IO] =
    (pairs: List[Rate.Pair]) => counter.update(_ + 1) *> IO.pure(f(pairs))
}
