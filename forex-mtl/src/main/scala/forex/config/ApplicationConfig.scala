package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    cache: CacheConfig,
    oneFrame: OneFrameConfig
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)

case class CacheConfig(
    ttl: FiniteDuration
)

case class OneFrameConfig(
  url: String
)
