package pureconfig

import com.typesafe.config.{ ConfigFactory, ConfigValueFactory, ConfigValueType }
import pureconfig.error.WrongType
import pureconfig.generic.error.NoValidEnumOptionFound
import pureconfig.generic.semiauto._
import shapeless.test.illTyped
import pureconfig.error.{ KeyNotFound, ConvertFailure, ConfigReaderFailures }

class EnumerationsSuite extends BaseSuite {

  sealed trait Color
  case object RainyBlue extends Color
  case object SunnyYellow extends Color

  val conf = ConfigFactory.parseString("{ type: person, name: John, surname: Doe }")

  behavior of "deriveEnumeration"

  it should "provide methods to derive readers for enumerations encoded as sealed traits" in {
    implicit val colorReader = deriveEnumerationReader[Color]

    ConfigReader[Color].from(ConfigValueFactory.fromAnyRef("rainy-blue")) shouldBe Right(RainyBlue)
    ConfigReader[Color].from(ConfigValueFactory.fromAnyRef("sunny-yellow")) shouldBe Right(SunnyYellow)

    val unknownValue = ConfigValueFactory.fromAnyRef("blue")
    ConfigReader[Color].from(unknownValue) should failWith(NoValidEnumOptionFound(unknownValue, Set("rainy-blue", "sunny-yellow"), "Color"))
    ConfigReader[Color].from(conf.root()) should failWith(WrongType(ConfigValueType.OBJECT, Set(ConfigValueType.STRING)), "", stringConfigOrigin(1))
  }

  it should "provide methods to derive writers for enumerations encoded as sealed traits" in {
    implicit val colorWriter = deriveEnumerationWriter[Color]

    ConfigWriter[Color].to(RainyBlue) shouldEqual ConfigValueFactory.fromAnyRef("rainy-blue")
    ConfigWriter[Color].to(SunnyYellow) shouldEqual ConfigValueFactory.fromAnyRef("sunny-yellow")
  }

  it should "provide methods to derive full converters for enumerations encoded as sealed traits" in {
    implicit val colorConvert = deriveEnumerationConvert[Color]

    ConfigConvert[Color].from(ConfigValueFactory.fromAnyRef("rainy-blue")) shouldBe Right(RainyBlue)
    ConfigConvert[Color].from(ConfigValueFactory.fromAnyRef("sunny-yellow")) shouldBe Right(SunnyYellow)
    ConfigConvert[Color].to(RainyBlue) shouldEqual ConfigValueFactory.fromAnyRef("rainy-blue")
    ConfigConvert[Color].to(SunnyYellow) shouldEqual ConfigValueFactory.fromAnyRef("sunny-yellow")
  }

  it should "provide customizable methods to derive readers for enumerations encoded as sealed traits" in {
    implicit val colorReader = deriveEnumerationReader[Color](ConfigFieldMapping(PascalCase, SnakeCase))

    ConfigReader[Color].from(ConfigValueFactory.fromAnyRef("rainy_blue")) shouldBe Right(RainyBlue)
    ConfigReader[Color].from(ConfigValueFactory.fromAnyRef("sunny_yellow")) shouldBe Right(SunnyYellow)
  }

  it should "provide customizable methods to derive writers for enumerations encoded as sealed traits" in {
    implicit val colorWriter = deriveEnumerationWriter[Color](ConfigFieldMapping(PascalCase, SnakeCase))

    ConfigWriter[Color].to(RainyBlue) shouldEqual ConfigValueFactory.fromAnyRef("rainy_blue")
    ConfigWriter[Color].to(SunnyYellow) shouldEqual ConfigValueFactory.fromAnyRef("sunny_yellow")
  }

  it should "provide customizable methods to derive full converters for enumerations encoded as sealed traits" in {
    implicit val colorConvert = deriveEnumerationConvert[Color](ConfigFieldMapping(PascalCase, SnakeCase))

    ConfigConvert[Color].from(ConfigValueFactory.fromAnyRef("rainy_blue")) shouldBe Right(RainyBlue)
    ConfigConvert[Color].from(ConfigValueFactory.fromAnyRef("sunny_yellow")) shouldBe Right(SunnyYellow)
    ConfigConvert[Color].to(RainyBlue) shouldEqual ConfigValueFactory.fromAnyRef("rainy_blue")
    ConfigConvert[Color].to(SunnyYellow) shouldEqual ConfigValueFactory.fromAnyRef("sunny_yellow")
  }

  it should "not allow deriving readers, writers and full converters for enumerations encoded as sealed traits whose subclasses are not all case objects" in {

    illTyped("deriveEnumerationReader[Entity]")
    illTyped("deriveEnumerationWriter[Entity]")
    illTyped("deriveEnumerationConvert[Entity]")
  }
}
