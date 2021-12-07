package uk.gov.hmrc.cdsreimbursementclaim.models.claim

import uk.gov.hmrc.cdsreimbursementclaim.models.claim.TaxCode._

object TaxCodes {

  val all: List[TaxCode] = List(
    A00,
    A20,
    A30,
    A35,
    A40,
    A45,
    B00,
    A50,
    A70,
    A80,
    A85,
    A90,
    A95,
    B05,
    NI407,
    NI411,
    NI412,
    NI413,
    NI415,
    NI419,
    NI421,
    NI422,
    NI423,
    NI425,
    NI429,
    NI431,
    NI433,
    NI435,
    NI438,
    NI440,
    NI441,
    NI442,
    NI443,
    NI444,
    NI445,
    NI446,
    NI447,
    NI451,
    NI461,
    NI462,
    NI463,
    NI473,
    NI481,
    NI483,
    NI485,
    NI487,
    NI511,
    NI520,
    NI521,
    NI522,
    NI540,
    NI541,
    NI542,
    NI546,
    NI551,
    NI556,
    NI561,
    NI570,
    NI571,
    NI572,
    NI589,
    NI591,
    NI592,
    NI595,
    NI597,
    NI611,
    NI615,
    NI619,
    NI623,
    NI627,
    NI633,
    NI99A,
    NI99B,
    NI99C,
    NI99D
  )

  private val stringToTaxCodeMap: Map[String, TaxCode] =
    all.map(code => code.value -> code).toMap

  def find(taxCode: String): Option[TaxCode] =
    stringToTaxCodeMap.get(taxCode)

  def findUnsafe(taxCode: String): TaxCode =
    stringToTaxCodeMap(taxCode)
}
