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

package uk.gov.hmrc.cdsreimbursementclaim.models

import ru.tinkoff.phobos.Namespace
import ru.tinkoff.phobos.decoding.{ElementDecoder, XmlDecoder}
import ru.tinkoff.phobos.derivation.semiauto._
import ru.tinkoff.phobos.encoding.{ElementEncoder, XmlEncoder}
import ru.tinkoff.phobos.syntax.xmlns

final case class soapenv()

case object soapenv {
  implicit val soapenvNs: Namespace[soapenv.type] = Namespace.mkInstance("http://schemas.xmlsoap.org/soap/envelope/")
  implicit val soapenvNss: Namespace[soapenv]     = Namespace.mkInstance("http://schemas.xmlsoap.org/soap/envelope/")
}

final case class HeadlessEnvelope[Body](@xmlns(soapenv) Body: Body)

object HeadlessEnvelope {
  implicit def deriveEnvelopeEncoder[Body : ElementEncoder]: XmlEncoder[HeadlessEnvelope[Body]] = {
    implicit val envelopeElementEncoder: ElementEncoder[HeadlessEnvelope[Body]] =
      deriveElementEncoder[HeadlessEnvelope[Body]]

    XmlEncoder.fromElementEncoderNs[HeadlessEnvelope[Body], soapenv]("Envelope")
  }

  implicit def deriveEnvelopeDecoder[Body : ElementDecoder]: XmlDecoder[HeadlessEnvelope[Body]] = {
    @SuppressWarnings(
      Array("org.wartremover.warts.Equals", "org.wartremover.warts.NonUnitStatements", "org.wartremover.warts.Var")
    )
    implicit val envelopeElementDecoder: ElementDecoder[HeadlessEnvelope[Body]] =
      deriveElementDecoder[HeadlessEnvelope[Body]]

    XmlDecoder.fromElementDecoderNs[HeadlessEnvelope[Body], soapenv]("Envelope")
  }
}
