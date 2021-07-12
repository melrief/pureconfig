package pureconfig

import com.typesafe.config.ConfigValueFactory
import shapeless.Witness
import shapeless.syntax.singleton._

import pureconfig.error.CannotConvert
import pureconfig.generic.singleton._

class SingletonSuite extends BaseSuite {
  val W = Witness

  val _42 = 42.narrow
  val config42 = ConfigValueFactory.fromAnyRef(_42)

  checkRead[W.`42`.T](config42 -> _42)
  checkWrite[W.`42`.T](_42 -> config42)

  it should "fail to read other integer values" in {
    forAll { i: Int =>
      whenever(i != _42) {
        ConfigReader[W.`42`.T].from(ConfigValueFactory.fromAnyRef(i)) should failWithReason[CannotConvert]
      }
    }
  }

  val _traverse = "traverse".narrow
  val configTraverse = ConfigValueFactory.fromAnyRef(_traverse)

  checkRead[W.`"traverse"`.T](configTraverse -> _traverse)
  checkWrite[W.`"traverse"`.T](_traverse -> configTraverse)

  it should "fail to read other string values" in {
    forAll { s: String =>
      whenever(s != _traverse) {
        ConfigReader[W.`"traverse"`.T].from(ConfigValueFactory.fromAnyRef(s)) should failWithReason[CannotConvert]
      }
    }
  }
}
