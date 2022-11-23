package uk.gov.hmrc.cdsreimbursementclaim.utils

import scala.io.Source
import scala.util.Try
import scala.util.Success

trait TestDataFromFile {

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.TryPartial",
      "org.wartremover.warts.Throw",
      "org.wartremover.warts.Equals"
    )
  )
  def testDataFromFile(filename: String): String = {
    val in = getClass.getResourceAsStream(s"${filename.drop("conf".size)}")
    if (in == null) throw new NullPointerException(s"Cannot read content of $filename")
    else
      Try(Source.fromInputStream(in).getLines.mkString)
        .transform(
          s => Success(s),
          e => Try { in.close(); throw e }
        )
        .get
  }

}
