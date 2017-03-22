package pureconfig

import org.scalatest.{ EitherValues, FlatSpec, Matchers }
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BaseSuite
  extends FlatSpec
  with ConfigConvertChecks
  with Matchers
  with EitherValues
  with GeneratorDrivenPropertyChecks
