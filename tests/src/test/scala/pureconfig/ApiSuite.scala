/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package pureconfig

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import com.typesafe.config.{ConfigFactory, ConfigValueType}
import pureconfig.PathUtils._
import scala.concurrent.duration.FiniteDuration

import pureconfig.error._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._

// We are testing deprecated methods, but we want to keep this in place to prevent regressions
// until we delete them. We need the annotation here to silence compiler warnings.
@deprecated("Construct a `ConfigSource` pipeline instead", "0.12.0")
class ApiSuite extends BaseSuite {

  behavior of "pureconfig"

  it should "loadConfig from reference.conf" in {
    case class Conf(d: Double, i: Int, s: String)
    loadConfig[Conf] shouldBe Right(Conf(0d, 0, "app_value"))
  }

  it should "loadConfig from reference.conf with a namespace" in {
    case class Conf(f: Float)
    loadConfig[Conf](namespace = "foo") shouldBe Right(Conf(3.0f))
  }

  it should "loadConfig config objects from a Typesafe Config" in {
    case class Conf(d: Double, i: Int)
    val conf = ConfigFactory.parseString("{ d: 0.5, i: 10 }")
    loadConfig[Conf](conf = conf) shouldBe Right(Conf(0.5d, 10))
  }

  it should "loadConfig config objects from a Typesafe Config with a namespace" in {
    case class Conf(f: Float)
    val conf = ConfigFactory.parseString("foo.bar { f: 1.0 }")
    loadConfig[Conf](conf = conf, namespace = "foo.bar") shouldBe Right(Conf(1.0f))
    loadConfig[Conf](conf = conf, namespace = "bar.foo") should failWith(
      KeyNotFound("bar", Set.empty),
      "",
      stringConfigOrigin(1)
    )
  }

  it should "loadConfig other values from a Typesafe Config with a namespace" in {
    val conf = ConfigFactory.parseString("foo { bar { f: 1.0 }, baz: 3.4 }")

    loadConfig[Float](conf = conf, namespace = "foo.bar.f") shouldBe Right(1.0f)

    loadConfig[Float](conf = conf, namespace = "foo.bar.h") should failWith(
      KeyNotFound("h", Set.empty),
      "foo.bar",
      stringConfigOrigin(1)
    )

    loadConfig[Float](conf = conf, namespace = "foo.baz.f") should failWith(
      WrongType(ConfigValueType.NUMBER, Set(ConfigValueType.OBJECT)),
      "foo.baz",
      stringConfigOrigin(1)
    )

    loadConfig[Float](conf = conf, namespace = "bar.foo.f") should failWith(
      KeyNotFound("bar", Set.empty),
      "",
      stringConfigOrigin(1)
    )

    loadConfig[Option[Float]](conf = conf, namespace = "foo.bar.f") shouldBe Right(Some(1.0f))

    // With the introduction of `ConfigSource`s we dropped support for reading missing
    // keys as `None` when an `Option[A]` is loaded as a root value
    // loadConfig[Option[Float]](conf = conf, namespace = "foo.bar.h") shouldBe Right(None)
    loadConfig[Option[Float]](conf = conf, namespace = "foo.bar.h") should failWith(
      KeyNotFound("h", Set.empty),
      "foo.bar",
      stringConfigOrigin(1)
    )

    loadConfig[Option[Float]](conf = conf, namespace = "foo.baz.f") should failWith(
      WrongType(ConfigValueType.NUMBER, Set(ConfigValueType.OBJECT)),
      "foo.baz",
      stringConfigOrigin(1)
    )

    loadConfig[Option[Float]](conf = conf, namespace = "bar.foo.f") should failWith(
      KeyNotFound("bar", Set.empty),
      "",
      stringConfigOrigin(1)
    )
  }

  it should "handle correctly namespaces with special chars" in {
    val conf = ConfigFactory.parseString(""" "fo.o" { "ba r" { f: 1.0 }, "ba z": 3.4 }""")

    loadConfig[Float](conf = conf, namespace = "\"fo.o\".\"ba r\".f") shouldBe Right(1.0f)

    loadConfig[Float](conf = conf, namespace = "\"fo.o\".\"ba r\".h") should failWith(
      KeyNotFound("h", Set.empty),
      "\"fo.o\".\"ba r\"",
      stringConfigOrigin(1)
    )

    loadConfig[Float](conf = conf, namespace = "\"fo.o\".\"ba z\".h") should failWith(
      WrongType(ConfigValueType.NUMBER, Set(ConfigValueType.OBJECT)),
      "\"fo.o\".\"ba z\"",
      stringConfigOrigin(1)
    )

    loadConfig[Float](conf = conf, namespace = "\"b.a.r\".foo.f") should failWith(
      KeyNotFound("b.a.r", Set.empty),
      "",
      stringConfigOrigin(1)
    )
  }

  it should "loadConfig from a configuration file" in {
    case class Conf(s: String, b: Boolean, sref: String) // sref defined in reference.conf
    val path = createTempFile("""{ b: true, s: "str" }""")
    loadConfig[Conf](path = path) shouldBe Right(Conf("str", true, "wow"))
    loadConfig[Conf](path = nonExistingPath) should failLike {
      case CannotReadFile(path, _) => be(path)(nonExistingPath)
    }
  }

  it should "loadConfig from a configuration file with a namespace" in {
    case class Conf(s: String, b: Boolean, sref: String) // foo.bar.sref defined in reference.conf
    val path = createTempFile("""foo.bar { b: true, s: "str" }""")
    loadConfig[Conf](path = path, namespace = "foo.bar") shouldBe Right(Conf("str", true, "foowow"))
    loadConfig[Conf](path = nonExistingPath, namespace = "foo.bar") should failLike {
      case CannotReadFile(path, _) => be(path)(nonExistingPath)
    }
    loadConfig[Conf](path = path, namespace = "bar.foo") should failWith(KeyNotFound("bar", Set.empty))
  }

  it should "be able to load a realistic configuration file" in {
    case class DriverConf(cores: Int, maxResultSize: String, memory: String)
    case class ExecutorConf(memory: String, extraJavaOptions: String)
    case class SparkAppConf(name: String)
    case class SparkLocalConf(dir: String)
    case class SparkNetwork(timeout: FiniteDuration)
    case class SparkConf(
        master: String,
        app: SparkAppConf,
        local: SparkLocalConf,
        driver: DriverConf,
        executor: ExecutorConf,
        extraListeners: Seq[String],
        network: SparkNetwork
    )
    case class SparkRootConf(spark: SparkConf)
    val configFile = createTempFile("""spark {
        |  app.name="myApp"
        |  master="local[*]"
        |  driver {
        |    maxResultSize="2g"
        |    memory="1g"
        |    cores="10"
        |  }
        |  executor {
        |    memory="2g"
        |    extraJavaOptions=""
        |  }
        |  extraListeners=[]
        |  local.dir="/tmp/"
        |  network.timeout=45s
        |}

        |// unused configuration
        |akka.loggers = ["akka.event.Logging$DefaultLogger"]""".stripMargin)

    implicit def productHint[A] = ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))
    val configOrError = loadConfig[SparkRootConf](configFile)

    val config = configOrError match {
      case Left(f) => fail(f.toString)
      case Right(c) => c
    }

    config.spark.executor.extraJavaOptions should be("")
    config.spark.driver.maxResultSize should be("2g")
    config.spark.extraListeners should be(Seq.empty[String])
    config.spark.app.name should be("myApp")
    config.spark.driver.memory should be("1g")
    config.spark.driver.cores should be(10)
    config.spark.master should be("local[*]")
    config.spark.executor.memory should be("2g")
    config.spark.local.dir should be("/tmp/")
    config.spark.network.timeout should be(FiniteDuration(45, TimeUnit.SECONDS))
  }

  "loadConfigFromFiles" should "load a complete configuration from a single file" in {
    case class Conf(b: Boolean, d: Double)
    val files = listResourcesFromNames("/conf/loadConfigFromFiles/priority2.conf")
    loadConfigFromFiles[Conf](files) shouldBe Right(Conf(false, 0.001d))
  }

  it should "fill in missing values from the lower priority files" in {
    case class Conf(f: Float)
    val files =
      listResourcesFromNames("/conf/loadConfigFromFiles/priority1.conf", "/conf/loadConfigFromFiles/priority2.conf")
    loadConfigFromFiles[Conf](files) shouldBe Right(Conf(0.99f))
  }

  it should "use an empty config if the list of files is empty" in {
    case class Conf(f: Float)
    val files = Set.empty[Path]
    loadConfigFromFiles[Conf](files) should failWithType[KeyNotFound] // f is missing
  }

  it should "merge reference.conf with the provided files" in {
    case class Conf(b: Boolean, d: Double, sref: String) // sref defined in reference.conf
    val files = listResourcesFromNames("/conf/loadConfigFromFiles/priority2.conf")
    loadConfigFromFiles[Conf](files) shouldBe Right(Conf(false, 0.001d, "wow"))
  }

  it should "ignore files that don't exist when failOnReadError is false" in {
    case class Conf(b: Boolean, d: Double)
    val files = listResourcesFromNames("/conf/loadConfigFromFiles/priority2.conf") :+ nonExistingPath
    loadConfigFromFiles[Conf](files) shouldBe Right(Conf(false, 0.001d))
  }

  it should "fail if any of the files doesn't exist and failOnReadError is true" in {
    case class Conf(f: Float)
    val files = listResourcesFromNames("/conf/loadConfigFromFiles/priority2.conf") :+ nonExistingPath
    loadConfigFromFiles[Conf](files, failOnReadError = true) should failWithType[CannotReadFile]
  }

  it should "use a namespace if given" in {
    case class Conf(f: Float)
    val files = listResourcesFromNames("/conf/loadConfigFromFiles/outerobject.conf")
    loadConfigFromFiles[Conf](files, namespace = "foo") shouldBe Right(Conf(3.0f))
  }

  "loadConfigWithFallback" should "fallback if no config keys are found" in {
    case class Conf(f: Float, o: Option[Int], d: Double)
    val priority1Conf = ConfigFactory.load("conf/loadConfigFromFiles/priority1.conf")
    // `f` and `o` are defined in priority1.conf, `d` is defined in reference.conf
    loadConfigWithFallback[Conf](priority1Conf) shouldBe Right(Conf(0.99f, None, 0.0))
  }
}
