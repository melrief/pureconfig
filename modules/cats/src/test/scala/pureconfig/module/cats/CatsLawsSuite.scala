package pureconfig.module.cats

import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.laws.discipline.SemigroupTests
import cats.laws.discipline._
import com.typesafe.config.ConfigValue
import org.scalatest.Matchers
import org.scalatest.funsuite.AnyFunSuite
import org.typelevel.discipline.scalatest.Discipline
import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.module.cats.arbitrary._
import pureconfig.module.cats.eq._
import pureconfig.module.cats.instances._

class CatsLawsSuite extends AnyFunSuite with Matchers with Discipline {
  checkAll("ConfigReader[Int]", ApplicativeErrorTests[ConfigReader, ConfigReaderFailures].applicativeError[Int, Int, Int])
  checkAll("ConfigWriter[Int]", ContravariantSemigroupalTests[ConfigWriter].contravariantSemigroupal[Int, Int, Int])
  checkAll("ConfigConvert[Int]", InvariantSemigroupalTests[ConfigConvert].invariantSemigroupal[Int, Int, Int])

  checkAll("ConfigValue", SemigroupTests[ConfigValue].semigroup)
  checkAll("ConfigReaderFailures", SemigroupTests[ConfigReaderFailures].semigroup)
}
