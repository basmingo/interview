package forex.http.rates

import forex.domain.Currency
import org.http4s.dsl.impl.OptionalQueryParamDecoderMatcher

object QueryParams {

  private[rates] def parseCurrency(raw: String): Either[String, Currency] =
    Currency.fromStringEither(raw)

  object FromQueryParam extends OptionalQueryParamDecoderMatcher[String]("from")
  object ToQueryParam extends OptionalQueryParamDecoderMatcher[String]("to")

}
