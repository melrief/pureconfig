package pureconfig

import com.typesafe.config.{ ConfigRenderOptions, ConfigValue, ConfigValueFactory }
import org.scalacheck.Arbitrary
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ EitherValues, FlatSpec, Matchers }
import pureconfig.error.ConfigReaderFailure

import scala.reflect.ClassTag

/**
 * Add utilities to a scalatest `FlatSpec` to test `ConfigConvert` instances
 */
trait ConfigConvertChecks { this: FlatSpec with Matchers with GeneratorDrivenPropertyChecks with EitherValues =>

  /**
   * For each value of type `T`, check that the value produced by converting to and then from `ConfigValue` is the same
   * of the original value
   *
   * Note that this method doesn't check all the values but only the values that can be created by `Arbitrary[T]` and
   * only the `ConfigValue` created by `ConfigConvert[T].to`. While `Arbitrary[T]` is usually comprehensive,
   * `ConfigConvert[T].from` could support different kind of values that `ConfigConvert[T].to` doesn't produce
   * because, for instance, multiple representation of `t: T` are possible. Use [[check()]] for those
   * representations.
   */
  def checkArbitrary[T](implicit cc: ConfigConvert[T], arb: Arbitrary[T], tag: ClassTag[T]): Unit =
    it should s"read an arbitrary ${tag.runtimeClass.getSimpleName}" in forAll {
      (t: T) => cc.from(cc.to(t)).right.value shouldEqual t
    }

  /**
   * A more generic version of [[checkArbitrary]] where the type which will be written as `ConfigValue` is
   * different from the type which will be read from that `ConfigValue`. The idea being is to test the reading
   * part of a `ConfigConvert` by providing another type for which it's easy to create `Arbitrary` instances
   * and write the values to a configuration.
   *
   * For instance, to test that `Double` can be read from percentages, like `"42 %"`, we can create a dummy
   * [[pureconfig.data.Percentage]] class which contains an integer from `0` to `100`, write that percentage to
   * a `ConfigValue` representing a `String` and then try to read the percentage from the `ConfigValue` via
   * `ConfigConvert[Double].from`. Creating an instance of `Arbitrary[Percentage]` is simple, same for
   * `ConfigConvert[Percentage]`.
   *
   * @param f a function used to convert a value of type `T2` to a value of type `T1`. The result of the conversion
   *          to and from a `ConfigValue` will be tested against the output of this function.
   * @param cr the `ConfigConvert` used to read a value from a `ConfigValue`. This is the instance that we want to test
   * @param cw the `ConfigConvert` used to write a value to a `ConfigValue`. This is the dummy instance used to test `cr`
   * @param arb the `Arbitrary` used to generate values to write a `ConfigValue` via `cw`
   */
  def checkArbitrary2[T1, T2](f: T2 => T1)(implicit cr: ConfigConvert[T1], cw: ConfigConvert[T2], arb: Arbitrary[T2], tag1: ClassTag[T1], tag2: ClassTag[T2]): Unit =
    it should s"read a ${tag1.runtimeClass.getSimpleName} from an arbitrary ${tag2.runtimeClass.getSimpleName}" in forAll {
      (t2: T2) => cr.from(cw.to(t2)).right.value shouldEqual f(t2)
    }

  /**
   * For each pair of value of type `T` and `ConfigValue`, check that `ConfigConvert[T].from`
   * successfully converts the latter into to former. Useful to test specific values
   */
  def check[T](valuesToReprs: (T, ConfigValue)*)(implicit cc: ConfigConvert[T], tag: ClassTag[T]): Unit =
    for ((value, repr) <- valuesToReprs) {
      it should s"read the value $value of type ${tag.runtimeClass.getSimpleName} " +
        s"from ${repr.render(ConfigRenderOptions.concise())}" in {
          cc.from(repr).right.value shouldEqual value
        }
    }

  /** Similar to [[check()]] but work on ConfigValues of type String */
  def checkString[T](valuesToStr: (T, String)*)(implicit cc: ConfigConvert[T], tag: ClassTag[T]): Unit =
    check[T](valuesToStr.map { case (t, s) => t -> ConfigValueFactory.fromAnyRef(s) }: _*)(cc, tag)

  /**
   * Check that `cc` returns error of type `E` wwhen trying to read each value passed with `values`
   *
   * @param values the values that should not be conver
   * @param cr the [[ConfigConvert]] to test
   */
  def checkFailure[T, E <: ConfigReaderFailure](values: ConfigValue*)(implicit cr: ConfigConvert[T], tag: ClassTag[T], eTag: ClassTag[E]): Unit =
    for (value <- values) {
      it should s"fail when it tries to read a value of type ${tag.runtimeClass.getSimpleName} " +
        s"from ${value.render(ConfigRenderOptions.concise())}" in {
          val result = cr.from(value)
          result shouldBe a[Left[_, _]]
          result.left.get.toList should have size 1
          result.left.get.head shouldBe a[E]
        }
    }
}
