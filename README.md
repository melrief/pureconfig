# PureConfig

<img src="docs/img/pureconfig-logo-1040x1200.png" width="130px" height="150px" align="right">

[![Build Status](https://travis-ci.org/pureconfig/pureconfig.svg?branch=master)](https://travis-ci.org/pureconfig/pureconfig)
[![Coverage Status](https://coveralls.io/repos/github/pureconfig/pureconfig/badge.svg?branch=master)](https://coveralls.io/github/pureconfig/pureconfig?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.pureconfig/pureconfig_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.pureconfig/pureconfig_2.11)
[![Join the chat at https://gitter.im/melrief/pureconfig](https://badges.gitter.im/melrief/pureconfig.svg)](https://gitter.im/melrief/pureconfig?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

PureConfig is a Scala library for loading configuration files. It reads [Typesafe Config](https://github.com/typesafehub/config) configurations written in [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md#hocon-human-optimized-config-object-notation), Java `.properties`, or JSON to native Scala classes in a boilerplate-free way. Sealed traits, case classes, collections, optional values, and many other [types are all supported out-of-the-box](#supported-types). Users also have many ways to add support for custom types or customize existing ones.

Click on the demo gif below to see how PureConfig effortlessly translates your configuration files to well-typed objects without error-prone boilerplate.
<br clear="right"> <!-- Turn off the wrapping for the logo image. -->

![](http://i.imgur.com/P6sda06.gif)


## Table of Contents

- [Why](#why)
- [Not Yet Another Configuration Library](#not-yet-another-configuration-library)
- [Add PureConfig to your project](#add-pureconfig-to-your-project)
- [Use PureConfig](#use-pureconfig)
- [Supported types](#supported-types)
- [Configurable converters](docs/configurable-converters.md)
- [Support new types](docs/support-new-types.md)
  - [Add support for simple types](docs/add-support-for-simple-types.md)
  - [Add support for complex types](docs/add-support-for-complex-types.md)
- [Override behaviour for types](docs/override-behaviour-for-types.md)
- [Override behaviour for case classes](docs/override-behaviour-for-case-classes.md)
  - [Field mappings](docs/override-behaviour-for-case-classes.md#field-mappings)
  - [Default field values](docs/override-behaviour-for-case-classes.md#default-field-values)
  - [Unknown keys](docs/override-behaviour-for-case-classes.md#unknown-keys)
- [Override behaviour for sealed families](docs/override-behaviour-for-sealed-families.md)
- [Error handling](docs/error-handling.md)
- [Handling missing keys](docs/handling-missing-keys.md)
- [Example](docs/example.md)
- [Whence the config files](docs/whence-the-config-files.md)
- [Support for Duration](docs/support-for-duration.md)
- [Integrating with other libraries](#integrating-with-other-libraries)
- [Contribute](#contribute)
- [FAQ](docs/faq.md)
- [License](#license)
- [Special thanks](#special-thanks)


## Why

Loading configurations has always been a tedious and error-prone procedure. A common way to do it
consists in writing code to deserialize each fields of the configuration. The more fields there are,
the more code must be written (and tested and maintained...) and this must be replicated for each project.

This kind of code is boilerplate because most of the times the code can be automatically generated by
the compiler based on what must be loaded. For instance, if you are going to load an `Int` for a field
named `foo`, then probably you want some code that gets the values associated with the key `foo` in
the configuration and assigns it to the proper field after converting it to `Int`.

The goal of this library is to create at compile-time the boilerplate necessary to load a configuration of a
certain type. In other words, you define **what** to load and PureConfig provides **how** to load it.


## Not yet another configuration library

PureConfig is not a configuration library in the sense that it doesn't search for files or parse them.
It can be seen as a better front-end for the existing libraries.
It uses the [Typesafe Config][typesafe-config] library for loading raw configurations and then
uses the raw configurations to do its magic.


## Add PureConfig to your project

In the sbt configuration file use Scala `2.10`, `2.11` or `2.12`:

```scala
scalaVersion := "2.12.3" // or "2.11.11", "2.10.6"
```

Add PureConfig to your library dependencies. For Scala `2.11` and `2.12`:

```scala
libraryDependencies ++= Seq(
  "com.github.pureconfig" %% "pureconfig" % "0.7.2"
)
```

For Scala `2.10` you need also the Macro Paradise plugin:

```scala
libraryDependencies ++= Seq(
  "com.github.pureconfig" %% "pureconfig" % "0.7.2",
  compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.patch)
)
```

For a full example of `build.sbt` you can have a look at this [build.sbt](https://github.com/melrief/pureconfig/blob/master/example/build.sbt)
used for the example.

Earlier versions of Scala had bugs which can cause subtle compile-time problems in PureConfig.
As a result we recommend only using 2.11.11 or 2.12.1 or newer within the minor series.

## Use PureConfig

Import the library package and use one of the `loadConfig` methods:

```scala
import pureconfig._
import pureconfig.error.ConfigReaderFailures

case class YourConfClass(name: String, quantity: Int)

val config: Either[pureconfig.error.ConfigReaderFailures,YourConfClass] = loadConfig[YourConfClass]
```


## Supported Types

Currently supported types for fields are:
- `String`, `Boolean`, `Double` (standard
  and percentage format ending with `%`), `Float` (also supporting percentage),
    `Int`, `Long`, `Short`, `URL`, `URI`, `Duration`, `FiniteDuration`;
- [`java.lang.Enum`](https://docs.oracle.com/javase/8/docs/api/java/lang/Enum.html)
- all collections implementing the `TraversableOnce` trait where the type of
  the elements is in this list;
- `Option` for optional values, i.e. values that can or cannot be in the configuration;
- `Map` with `String` keys and any value type that is in this list;
- everything in [`java.time`](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html) (must be
  configured first - see [Configurable converters](docs/configurable-converters.md));
- [`java.io.File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html);
- [`java.util.UUID`](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html);
- [`java.nio.file.Path`](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html);
- [`java.util.regex.Pattern`](https://docs.oracle.com/javase/8/docs/api/index.html?java/util/regex/Pattern.html) and [`scala.util.matching.Regex`](https://www.scala-lang.org/files/archive/api/current/scala/util/matching/Regex.html);
- [`java.math.BigDecimal`](https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html), [`java.math.BigInteger`](https://docs.oracle.com/javase/8/docs/api/java/math/BigInteger.html),
  [`scala.math.BigDecimal`](https://www.scala-lang.org/api/2.12.2/index.html#scala.math.BigDecimal), and [`scala.math.BigInt`](https://www.scala-lang.org/api/2.12.2/index.html#scala.math.BigInt);
- Typesafe `ConfigValue`, `ConfigObject` and `ConfigList`;
- value classes for which readers and writers of the inner type are used;
- case classes;
- sealed families of case classes (ADTs).

# Example

First, import the library, define data types, and a case class to hold the configuration:

```scala
import com.typesafe.config.ConfigFactory.parseString
import pureconfig.loadConfig

sealed trait MyAdt
case class AdtA(a: String) extends MyAdt
case class AdtB(b: Int) extends MyAdt
final case class Port(value: Int) extends AnyVal
case class MyClass(
  boolean: Boolean,
  port: Port,
  adt: MyAdt,
  list: List[Double],
  map: Map[String, String],
  option: Option[String])
```

Then, load the configuration (in this case from a hard-coded string):

```scala
val conf = parseString("""{ 
  "boolean": true,
  "port": 8080, 
  "adt": { 
    "type": "adtb", 
    "b": 1 
  }, 
  "list": ["1", "20%"], 
  "map": { "key": "value" } 
}""")
// conf: com.typesafe.config.Config = Config(SimpleConfigObject({"adt":{"b":1,"type":"adtb"},"boolean":true,"list":["1","20%"],"map":{"key":"value"},"port":8080}))

loadConfig[MyClass](conf)
// res3: Either[pureconfig.error.ConfigReaderFailures,MyClass] = Right(MyClass(true,Port(8080),AdtB(1),List(1.0, 0.2),Map(key -> value),None))
```


## Integrating with other libraries

### Internal Modules

The core of PureConfig eschews unnecessary dependencies. Separate modules exist to support types which are not part of the standard Scala and Java libraries.

- [`pureconfig-akka`](modules/akka) provides convererts for [Akka](http://akka.io/) data types.
- [`pureconfig-cats`](modules/cats) provides converters for [Cats](http://typelevel.org/cats/) data structures and Cats typeclass instances.
- [`pureconfig-enum`](modules/enum/) provides converters for enums generated by [julienrf's enum library](https://github.com/julienrf/enum).
- [`pureconfig-enumeratum`](modules/enumeratum/) provides converters for enums generated by [Enumeratum](https://github.com/lloydmeta/enumeratum).
- [`pureconfig-javax`](modules/javax/) provides converters for value classes in the javax packages.
- [`pureconfig-joda`](modules/joda/) provides configurable converters for [Joda Time](http://www.joda.org/joda-time/) types.
- [`pureconfig-scala-xml`](modules/scala-xml) provides support for XML via [Scala XML](https://github.com/scala/scala-xml).
- [`pureconfig-squants`](modules/squants/) provides converters for [Squants](http://www.squants.com/)'s beautiful types representing units of measure.

### External Integrations

A non-comprehensive list of other libraries which have integrated with PureConfig to provide a richer experience include:

- `refined-pureconfig` allows PureConfig to play nicely with [`refined`](https://github.com/fthomas/refined/)'s type refinements. Viktor Lövgren's blog post gleefully explains how [PureConfig and refined work together](https://blog.vlovgr.se/posts/2016-12-24-refined-configuration.html).

## Contribute

PureConfig is a free library developed by several people around the world.
Contributions are welcomed and encouraged. If you want to contribute, we suggest to have a look at the
[available issues](https://github.com/melrief/pureconfig/issues) and to talk with
us on the [pureconfig gitter channel](https://gitter.im/melrief/pureconfig?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge).

If you'd like to add support for types which are not part the standard Java or Scala libraries, please consider submitting a pull request to create a [module](#internal-modules). [Pull Request #108](https://github.com/melrief/pureconfig/pull/108/files) created a very simple module. It should provide a good template for the pieces you'll need to add.

The steps to create a new module, called _`nexttopmod`_, are:

1. Define a new project in the root `build.sbt`. There are other examples near the top of the file.
2. Create a new  `modules/nexttopmod/` subdirectory.
3. Add a `modules/nexttopmod/build.sbt` defining the module's name and special dependencies.
4. Implement converters. Typically they're in a `package object` in `modules/nexttopmod/src/main/scala/pureconfig/module/nexttopmod/package.scala`.
5. Test the converters. Usually tests would be in `modules/nexttopmod/src/test/scala/pureconfig/module/nexttopmod/NextTopModSuite.scala`.
6. Optionally explain a little bit about how it works in `modules/nexttopmod/README.md`.

PureConfig supports the [Typelevel](http://typelevel.org/) [code of conduct](http://typelevel.org/conduct.html) and wants all of its channels (Gitter, GitHub, etc.) to be
welcoming environments for everyone.


## License

[Mozilla Public License, version 2.0](https://github.com/melrief/pureconfig/blob/master/LICENSE)


## Special Thanks

To the [Shapeless](https://github.com/milessabin/shapeless) and to the [Typesafe Config](https://github.com/typesafehub/config)
developers.

[typesafe-config]: https://github.com/typesafehub/config
