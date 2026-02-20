package forex.domain

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  )

  def allPairs(currencies: List[Currency]): List[Pair] =
    currencies.flatMap { from =>
      currencies.collect {
        case to if to != from => Pair(from, to)
      }
    }
}
