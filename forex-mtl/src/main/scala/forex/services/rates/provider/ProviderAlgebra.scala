package forex.services.rates.provider

import forex.domain.Rate
import forex.services.rates.errors._

trait ProviderAlgebra[F[_]] {
  def get(pair: Rate.Pair): F[Error Either Rate]
}
