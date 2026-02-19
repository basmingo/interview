package forex.services.rates.provider

import forex.domain.Rate
import forex.services.rates.errors._

trait Algebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}
