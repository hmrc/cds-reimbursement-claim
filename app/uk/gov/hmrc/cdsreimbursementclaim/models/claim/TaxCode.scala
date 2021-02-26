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

package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import julienrf.json.derived
import play.api.libs.json.OFormat

//TODO: convert to implicit tostring method
sealed trait TaxCode extends Product with Serializable {
  def description: String
}

object TaxCode {
  sealed trait UKTaxCode extends TaxCode with Product with Serializable {
    override def description: String
  }

  object UKTaxCode {
    case object A00 extends UKTaxCode {
      override def description: String = "A00"
    }

    case object A20 extends UKTaxCode {
      override def description: String = "A20"
    }

    case object A30 extends UKTaxCode {
      override def description: String = "A30"
    }

    case object A35 extends UKTaxCode {
      override def description: String = "A35"
    }

    case object A40 extends UKTaxCode {
      override def description: String = "A40"
    }

    case object A45 extends UKTaxCode {
      override def description: String = "A45"
    }

    case object B00 extends UKTaxCode {
      override def description: String = "B00"
    }

    implicit val format: OFormat[UKTaxCode] = derived.oformat[UKTaxCode]()
  }

  sealed trait EUTaxCode extends TaxCode with Product with Serializable {
    def description: String
  }

  object EUTaxCode {
    case object A50 extends EUTaxCode {
      override def description: String = "A50"
    }
    case object A70 extends EUTaxCode {
      override def description: String = "A70"
    }
    case object A80 extends EUTaxCode {
      override def description: String = "A80"
    }
    case object A85 extends EUTaxCode {
      override def description: String = "A85"
    }
    case object A90 extends EUTaxCode {
      override def description: String = "A90"
    }
    case object A95 extends EUTaxCode {
      override def description: String = "A95"
    }
    case object B05 extends EUTaxCode {
      override def description: String = "B05"
    }

    implicit val format: OFormat[EUTaxCode] = derived.oformat[EUTaxCode]()

  }

  implicit val format: OFormat[TaxCode] = derived.oformat[TaxCode]()

}
