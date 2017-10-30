---
layout: home
---
# PureConfig

<img src="img/pureconfig-logo-1040x1200.png" width="130px" height="150px" align="right">

[![Build Status](https://travis-ci.org/pureconfig/pureconfig.svg?branch=master)](https://travis-ci.org/pureconfig/pureconfig)
[![Coverage Status](https://coveralls.io/repos/github/pureconfig/pureconfig/badge.svg?branch=master)](https://coveralls.io/github/pureconfig/pureconfig?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.pureconfig/pureconfig_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.pureconfig/pureconfig_2.11)
[![Join the chat at https://gitter.im/melrief/pureconfig](https://badges.gitter.im/melrief/pureconfig.svg)](https://gitter.im/melrief/pureconfig?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

PureConfig is a Scala library for loading configuration files. It reads [Typesafe Config](https://github.com/typesafehub/config) configurations written in [HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md#hocon-human-optimized-config-object-notation), Java `.properties`, or JSON to native Scala classes in a boilerplate-free way. Sealed traits, case classes, collections, optional values, and many other [types are all supported out-of-the-box](#supported-types). Users also have many ways to add support for custom types or customize existing ones.

Click on the demo gif below to see how PureConfig effortlessly translates your configuration files to well-typed objects without error-prone boilerplate.
<br clear="right"> <!-- Turn off the wrapping for the logo image. -->

<img src="http://i.imgur.com/P6sda06.gif" style="width: 100%;" />

## Documentation

Read the [documentation](setup-pureconfig.html).

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

## Contribute

PureConfig is a free library developed by several people around the world.
Contributions are welcomed and encouraged. If you want to contribute, we suggest to have a look at the
[available issues](https://github.com/pureconfig/pureconfig/issues) and to talk with
us on the [pureconfig gitter channel](https://gitter.im/pureconfig/pureconfig?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge).

If you'd like to add support for types which are not part the standard Java or Scala libraries, please consider submitting a pull request to create a [module](#internal-modules). [Pull Request #108](https://github.com/pureconfig/pureconfig/pull/108/files) created a very simple module. It should provide a good template for the pieces you'll need to add.

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

[Mozilla Public License, version 2.0](https://github.com/pureconfig/pureconfig/blob/master/LICENSE)


## Special Thanks

To the [Shapeless](https://github.com/milessabin/shapeless) and to the [Typesafe Config](https://github.com/typesafehub/config)
developers.

[typesafe-config]: https://github.com/typesafehub/config