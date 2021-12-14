package uk.gov.hmrc.cdsreimbursementclaim.utils

import play.api.libs.json.Format

object EnumerationFormat {

  def apply[T](mappings: Map[String, T]): Format[T] =
    SimpleStringFormat(mappings(_), (value: T) => value.toString)
}
