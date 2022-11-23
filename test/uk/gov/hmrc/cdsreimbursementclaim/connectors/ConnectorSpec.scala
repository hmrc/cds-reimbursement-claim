package uk.gov.hmrc.cdsreimbursementclaim.connectors

import org.scalatest.Inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.cdsreimbursementclaim.utils.{EndpointStub}

trait ConnectorSpec
    extends AnyWordSpec
    with Matchers
    with Inside
    with EndpointStub
    with FutureAwaits
    with DefaultAwaitTimeout
