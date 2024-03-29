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

package uk.gov.hmrc.cdsreimbursementclaim.metrics

import com.codahale.metrics.{Counter, Timer}
import com.google.inject.{Inject, Singleton}
import com.codahale.metrics.MetricRegistry

@Singleton
class Metrics @Inject() (metricRegistry: MetricRegistry) {

  protected def timer(name: String): Timer = metricRegistry.timer(s"backend.$name")

  protected def counter(name: String): Counter = metricRegistry.counter(s"backend.$name")

  val submitClaimTimer: Timer = timer("submit-claim.time")

  val submitClaimErrorCounter: Counter = counter("submit-claim.errors.count")

  val submitClaimConfirmationEmailTimer: Timer = timer("email.submit-claim-confirmation.time")

  val submitClaimConfirmationEmailErrorCounter: Counter = counter("email.submit-claim-confirmation.errors.count")

}
