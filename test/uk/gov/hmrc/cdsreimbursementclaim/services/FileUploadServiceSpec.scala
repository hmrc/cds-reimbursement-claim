/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.services

import cats.data.EitherT
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.cdsreimbursementclaim.connectors.FileUploadConnector
import uk.gov.hmrc.cdsreimbursementclaim.models.GenerateUploadFiles._
import uk.gov.hmrc.cdsreimbursementclaim.models.dec64.Dec64Body
import uk.gov.hmrc.cdsreimbursementclaim.models.{Error, HeadlessEnvelope}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FileUploadServiceSpec extends AnyWordSpec with Matchers with MockFactory {

  val fileUploadConnector = mock[FileUploadConnector]
  val fileUploadService   = new FileUploadServiceImpl(fileUploadConnector)

  implicit val request: Request[_] = FakeRequest()
  implicit val hc: HeaderCarrier   = HeaderCarrier()

  def mockfileUpload(response: Either[Error, HttpResponse]) =
    (fileUploadConnector
      .upload(_: String)(_: HeaderCarrier))
      .expects(*, *)
      .returning(EitherT.fromEither[Future](response))
      .atLeastOnce()

  "FileUploadService" should {
    "return nothing in case of 204 (No-Content) response" in {
      val dec64 = sample[Dec64Body]
      val soap  = Dec64Body.soapEncoder.encode(HeadlessEnvelope(dec64))
      mockfileUpload(Right(HttpResponse(204, "")))
      await(fileUploadService.upload(soap).value) shouldBe Right(())
    }

    "return error in case of not 204 (No-Content) response" in {
      val dec64 = sample[Dec64Body]
      val soap  = Dec64Body.soapEncoder.encode(HeadlessEnvelope(dec64))
      mockfileUpload(Right(HttpResponse(200, "Wrong")))
      await(fileUploadService.upload(soap).value) shouldBe Left(
        Error("Call to submit claim came back with status 200, body: Wrong")
      )
    }
  }
}
