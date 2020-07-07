package pureconfig.generic.error

import com.typesafe.config.{ ConfigRenderOptions, ConfigValue }
import pureconfig.error.FailureReason

final case class NoValidEnumOptionFound(value: ConfigValue, validOptions: Set[String], enumType: String) extends FailureReason {
  def description =
    s"Cannot convert ${value.render(ConfigRenderOptions.concise())} to a $enumType. " +
      s"The valid options are:\n" +
      validOptions.toSeq.sorted.map(o => s"  - $o").mkString("\n")
}
