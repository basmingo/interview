package forex.services.provider

import forex.domain.Rate
import forex.services.rates.errors._

trait ProviderAlgebra[F[_]] {
  def get(pairs: List[Rate.Pair]): F[Error Either Map[Rate.Pair, Rate]]
}
