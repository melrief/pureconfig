
# Http4s Blaze client module for PureConfig

Adds support for [Http4s](http://http4s.org/)'s Blaze client to PureConfig.
It provides a `ConfigReader` for custom `BlazeClientBuilderConfig`,
which in turn creates `BlazeClientBuilder` via the `configure` method.

## Add pureconfig-http4s-blaze-client to your project

In addition to [core PureConfig](https://github.com/pureconfig/pureconfig), you'll need:

```scala
libraryDependencies += "com.github.pureconfig" %% "pureconfig-http4s-blaze-client" % "0.12.2"
```

## Example

```scala
import java.util.concurrent.TimeUnit

import cats.effect.{ContextShift, IO}
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.syntax._
import pureconfig.module.http4s.blaze.client._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration
```

We can read a `BlazeServerBuilderConfig` and create a client with the following code:

```scala
implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

val conf = ConfigFactory.parseString(s"""{ responseHeaderTimeout: "60 s" }""")
val config = conf.to[BlazeClientBuilderConfig].right.value

val client = config.toBuilder[IO](global).resource
```
