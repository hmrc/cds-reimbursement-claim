/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.cdsreimbursementclaim.models.ccs

import ru.tinkoff.phobos

object Namespaces {
  case object soap
  implicit val soapNamespace: phobos.Namespace[soap.type] =
    phobos.Namespace.mkInstance("http://schemas.xmlsoap.org/soap/envelope/")
  case object xs
  implicit val xsNamespace: phobos.Namespace[xs.type]     = phobos.Namespace.mkInstance("http://www.w3.org/2001/XMLSchema")
  case object vc
  implicit val vcNamespace: phobos.Namespace[vc.type]     =
    phobos.Namespace.mkInstance("http://www.w3.org/2007/XMLSchema-versioning")
  case object mdg
  implicit val mdgNamespace: phobos.Namespace[mdg.type]   =
    phobos.Namespace.mkInstance("http://www.hmrc.gsi.gov.uk/mdg/batchFileInterfaceMetadataSchema")
}
