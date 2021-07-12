
# Http4s Blaze server module for PureConfig

Adds support for [Http4s](http://http4s.org/)'s Blaze server to PureConfig.
It provides a `ConfigReader` for custom `BlazeServerBuilderConfig`,
which in turn creates `BlazeServerBuilder` via the `configure` method.

## Add pureconfig-http4s-blaze-server to your project

In addition to [core PureConfig](https://github.com/pureconfig/pureconfig), you'll need:

```scala
libraryDependencies += "com.github.pureconfig" %% "pureconfig-http4s-blaze-server" % "0.12.2"
```

## Example

```scala
import cats.effect.{ContextShift, IO, Timer}
import com.typesafe.config.ConfigFactory
import pureconfig._
import pureconfig.syntax._
import pureconfig.module.http4s.blaze.server._

import scala.concurrent.ExecutionContext.global
```

We can read a `BlazeServerBuilderConfig` and create a server with the following code:

```scala
implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
implicit val timer: Timer[IO] = IO.timer(global)

val config = conf.to[BlazeServerBuilderConfig].right.value

val server = config.toBuilder[IO]().resource
```
