# PureConfig

A boilerplate-free Scala library for loading configuration files.

[![Build Status](https://travis-ci.org/melrief/pureconfig.svg?branch=master)](https://travis-ci.org/melrief/pureconfig)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.melrief/pureconfig_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.melrief/pureconfig_2.11)
[![Join the chat at https://gitter.im/melrief/pureconfig](https://badges.gitter.im/melrief/pureconfig.svg)](https://gitter.im/melrief/pureconfig?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

![](http://i.imgur.com/S5QUS8c.gif)


## Table of Contents

- [Why](#why)
- [Not Yet Another Configuration Library](#not-yet-another-configuration-library)
- [Add PureConfig to your project](#add-pureconfig-to-your-project)
- [Use PureConfig](#use-pureconfig)
- [Supported types](#supported-types)
- [Configurable converters](core/docs/configurable-converters.md)
- [Support new types](core/docs/support-new-types.md)
- [Override behaviour for types](core/docs/override-behaviour-for-types.md)
- [Override behaviour for case classes](core/docs/override-behaviour-for-case-classes.md)
  - [Field mappings](core/docs/override-behaviour-for-case-classes.md#field-mappings)
  - [Default field values](core/docs/override-behaviour-for-case-classes.md#default-field-values)
  - [Unknown keys](core/docs/override-behaviour-for-case-classes.md#unknown-keys)
- [Override behaviour for sealed families](core/docs/override-behaviour-for-sealed-families.md)
- [Handling missing keys](core/docs/handling-missing-keys.md)
- [Example](core/docs/example.md)
- [Whence the config files](core/docs/whence-the-config-files.md)
- [Contribute](#contribute)
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
scalaVersion := "2.12.1" // or "2.11.8", "2.10.5"
```

Add PureConfig to your library dependencies. For Scala `2.11` and `2.12`:

```scala
libraryDependencies ++= Seq(
  "com.github.melrief" %% "pureconfig" % "0.5.1"
)
```

For Scala `2.10` you need also the Macro Paradise plugin:

```scala
libraryDependencies ++= Seq(
  "com.github.melrief" %% "pureconfig" % "0.5.1",
  compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
)
```

For a full example of `build.sbt` you can have a look at this [build.sbt](https://github.com/melrief/pureconfig/blob/master/example/build.sbt)
used for the example.


## Use PureConfig

Import the library package and use one of the `loadConfig` methods:

```scala
import pureconfig._

val config: Try[YourConfClass] = loadConfig[YourConfClass]
```


## Supported Types

Currently supported types for fields are:
- `String`, `Boolean`, `Double` (standard
  and percentage format ending with `%`), `Float` (also supporting percentage),
  `Int`, `Long`, `Short`, `URL`, `Duration`, `FiniteDuration`;
- all collections implementing the `TraversableOnce` trait where the type of
  the elements is in this list;
- `Option` for optional values, i.e. values that can or cannot be in the configuration;
- `Map` with `String` keys and any value type that is in this list;
- everything in [`java.time`](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html) (must be
  configured first - see [Configurable converters](#configurable-converters));
- [`java.util.UUID`](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html);
- [`java.nio.file.Path`](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html)
- Typesafe `ConfigValue`, `ConfigObject` and `ConfigList`;
- case classes;
- sealed families of case classes (ADTs).

An almost comprehensive example is:

```scala
import com.typesafe.config.ConfigFactory.parseString
import pureconfig.loadConfig

sealed trait MyAdt
case class AdtA(a: String) extends MyAdt
case class AdtB(b: Int) extends MyAdt
case class MyClass(int: Int, adt: MyAdt, list: List[Double], map: Map[String, String], option: Option[String])

val conf = parseString("""{ "int": 1, "adt": { "type": "adtb", "b": 1 }, "list": ["1", "20%"], "map": { "key": "value" } }""")

loadConfig[MyClass](conf)
// returns Success(MyClass(1, AdtB(1), List(1.0, 0.2), Map("key" -> "value"), None))
```


## Contribute

PureConfig is a free library developed by several people around the world.
Contributions are welcomed and encouraged. If you want to contribute, we suggest to have a look at the
[available issues](https://github.com/melrief/pureconfig/issues) and to talk with
us on the [pureconfig gitter channel](https://gitter.im/melrief/pureconfig?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge).

PureConfig supports the [Typelevel](http://typelevel.org/) [code of conduct](http://typelevel.org/conduct.html) and wants all of its channels (Gitter, GitHub, etc.) to be
welcoming environments for everyone.


## License

[Mozilla Public License, version 2.0](https://github.com/melrief/pureconfig/blob/master/LICENSE)


## Special Thanks

To the [Shapeless](https://github.com/milessabin/shapeless) and to the [Typesafe Config](https://github.com/typesafehub/config)
developers.

[typesafe-config]: https://github.com/typesafehub/config
