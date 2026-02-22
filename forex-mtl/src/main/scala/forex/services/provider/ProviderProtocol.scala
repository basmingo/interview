package forex.services.provider

object ProviderProtocol {

  final case class OneFrameErrorResponse(
      error: String
  )

  final case class OneFrameRateResponse(
      from: String,
      to: String,
      price: BigDecimal,
      timeStamp: String
  )

}
