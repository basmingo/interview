package forex.services.rates.provider.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors._
import forex.services.rates.provider.Algebra

class OneFrameStub[F[_]: Applicative](oneFrameUrl: String) extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]
}
