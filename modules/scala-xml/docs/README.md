# Scala-XML module for PureConfig

Adds support for XML via [Scala XML](https://github.com/scala/scala-xml) to PureConfig.

## Add pureconfig-scala-xml to your project

In addition to [core pureconfig](https://github.com/pureconfig/pureconfig), you'll need:

```scala
libraryDependencies += "com.github.pureconfig" %% "pureconfig-scala-xml" % "@VERSION@"
```

## Example

To load an `Elem` into a configuration, we'll create a class to hold our configuration:

```scala mdoc:silent
import scala.xml.Elem
import com.typesafe.config.ConfigFactory.parseString
import pureconfig._
import pureconfig.generic.auto._
import pureconfig.module.scalaxml._

case class Config(people: Elem)
```

We can read a `Config` like:
```scala mdoc:to-string
val conf = parseString(
  s"""{ people =
    |   \"\"\"<people>
    |      <person firstName="A" lastName="Person" />
    |      <person firstName="Another" lastName="Person" />
    |    </people>\"\"\"
    |}""".stripMargin)
ConfigSource.fromConfig(conf).load[Config]
```

## Notes

All XML values are deserialized into `Elem`.

In HOCON files, if the XML contains quotes, you will need to either wrap the XML in triple-quotes or escape the
embedded quotes.
