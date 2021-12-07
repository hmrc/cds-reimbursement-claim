package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.DateGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, PostNewClaimsRequest, RequestCommon, RequestDetail, RequestDetailA, RequestDetailB}
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.UUIDGenerator

object TPI05 {

  def request: Builder = Builder()

  final case class Builder() {//(details: RequestDetail) extends AnyVal {

    def verify: Either[Error, EisSubmitClaimRequest] = {
      EisSubmitClaimRequest(
        PostNewClaimsRequest(
          RequestCommon(
            originatingSystem = Platform.MDTP,
            receiptDate = DateGenerator.nextReceiptDate,
            acknowledgementReference = UUIDGenerator.compactCorrelationId
          ),
          RequestDetail(
            null, null
//            RequestDetailA(
//            ),
//            RequestDetailB(
//              ???
//            )
          )
        )
      )

      ???
    }
  }
}
