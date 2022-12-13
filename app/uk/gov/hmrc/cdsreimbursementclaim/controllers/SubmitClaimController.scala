/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cdsreimbursementclaim.controllers

import cats.data.EitherT
import cats.implicits.toFlatMapOps
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.cdsreimbursementclaim.controllers.actions.AuthenticateWithUserActions
import uk.gov.hmrc.cdsreimbursementclaim.models.Error
import uk.gov.hmrc.cdsreimbursementclaim.models.claim._
import uk.gov.hmrc.cdsreimbursementclaim.services.ClaimService
import uk.gov.hmrc.cdsreimbursementclaim.services.ccs.{CcsSubmissionService, ClaimToDec64Mapper}
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging
import uk.gov.hmrc.cdsreimbursementclaim.utils.Logging.LoggerOps
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SubmitClaimController @Inject() (
  authenticate: AuthenticateWithUserActions,
  claimService: ClaimService,
  ccsSubmissionService: CcsSubmissionService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  final val submitC285Claim: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[C285ClaimRequest] {
      uploadDocumentsOnce {
        claimService.submitC285Claim(_)
      }
    }
  }

  final val submitSingleOverpaymentsClaim: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[SingleOverpaymentsClaimRequest] {
      uploadDocumentsOnce {
        claimService.submitSingleOverpaymentsClaim(_)
      }
    }
  }

  final val submitSingleRejectedGoodsClaim: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[RejectedGoodsClaimRequest[SingleRejectedGoodsClaim]] {
      uploadDocumentsOnce {
        claimService.submitRejectedGoodsClaim(_)
      }
    }
  }

  final val submitMultipleRejectedGoodsClaim: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[RejectedGoodsClaimRequest[MultipleRejectedGoodsClaim]] {
      uploadDocumentsOnce {
        claimService.submitMultipleRejectedGoodsClaim(_)
      }
    }
  }

  final val submitScheduledRejectedGoodsClaim: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[RejectedGoodsClaimRequest[ScheduledRejectedGoodsClaim]] {
      uploadDocumentsOnce {
        claimService.submitScheduledRejectedGoodsClaim(_)
      }
    }
  }

  final val submitSecuritiesClaim: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[SecuritiesClaimRequest] {
      uploadDocumentsOnce {
        claimService.submitSecuritiesClaim(_)
      }
    }
  }

  final val submitFiles: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    withJsonBody[Dec64UploadRequest] {
      uploadDocumentsOnce { uploadFilesRequest =>
        EitherT.pure[Future, Error](ClaimSubmitResponse(uploadFilesRequest.caseNumber))
      }
    }.map { _ =>
      Accepted
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  private def uploadDocumentsOnce[R](
    submit: R => EitherT[Future, Error, ClaimSubmitResponse]
  )(implicit hc: HeaderCarrier, claimToDec64Mapper: ClaimToDec64Mapper[R]): R => Future[Result] =
    request =>
      submit(request)
        .flatTap { response =>
          ccsSubmissionService.enqueue(request, response)
        }
        .map { submitClaimResponse =>
          Ok(Json.toJson(submitClaimResponse))
        }
        .valueOr { error =>
          logger.warn("Error submitting claim", error)
          InternalServerError
        }
}
