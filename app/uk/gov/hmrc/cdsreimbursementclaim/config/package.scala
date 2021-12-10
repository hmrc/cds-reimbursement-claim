package uk.gov.hmrc.cdsreimbursementclaim

import play.api.Configuration
import configs.syntax._

package object config {

  implicit class ConfigurationOps(val configuration: Configuration) extends AnyVal {
    def whetherCorrectAdditionalInformationMapping: Boolean =
      configuration.underlying
        .get[Boolean](s"feature.enable-correct-additional-information-code-mapping")
        .value
  }
}
