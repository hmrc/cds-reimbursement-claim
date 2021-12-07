package uk.gov.hmrc.cdsreimbursementclaim.services.tpi05

import uk.gov.hmrc.cdsreimbursementclaim.config.MetaConfig.Platform
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim.{C285Claim, SubmitClaimRequest}
import uk.gov.hmrc.cdsreimbursementclaim.models.dates.DateGenerator
import uk.gov.hmrc.cdsreimbursementclaim.models.eis.claim.{EisSubmitClaimRequest, PostNewClaimsRequest, RequestCommon, RequestDetail}
import uk.gov.hmrc.cdsreimbursementclaim.models.ids.UUIDGenerator
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging

class C285ClaimToTPI05Mapper extends ClaimToTPI05Mapper[C285Claim] with Logging {

  def mapToEisSubmitClaimRequest(claim: C285Claim): Either[Error, EisSubmitClaimRequest] = {
    val requestCommon = RequestCommon(
      originatingSystem = Platform.MDTP,
      receiptDate = DateGenerator.nextReceiptDate,
      acknowledgementReference = UUIDGenerator.compactCorrelationId
    )

    TPI05Request.builder
      .withMrnDertails()
      .buildOrFail
      .fold(

      )

    buildMrnNumberPayload(submitClaimRequest).bimap(
      error => Error(s"validation errors: ${error.toString}"),
      requestDetail =>
        EisSubmitClaimRequest(
          PostNewClaimsRequest(
            requestCommon = requestCommon,
            requestDetail = requestDetail
          )
        )
    )
  }
}
