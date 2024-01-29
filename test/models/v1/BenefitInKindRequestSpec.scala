/*
 * Copyright 2024 HM Revenue & Customs
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

package models.v1

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class BenefitInKindRequestSpec extends AnyWordSpec with Matchers {
  "BenefitInKindRequest" when {
    ".equals" must {

      "return true if 2 BenefitInKindRequest instances have only the same iabdType" in {
        val bik: BenefitInKindRequest          = BenefitInKindRequest(
          IabdType.CarBenefit,
          PbikAction.ReinstatePayrolledBenefitInKind,
          isAgentSubmission = false
        )
        val bikToCompare: BenefitInKindRequest = BenefitInKindRequest(
          IabdType.CarBenefit,
          PbikAction.RemovePayrolledBenefitInKind,
          isAgentSubmission = true
        )

        bik.equals(bikToCompare) mustBe true
      }
    }

    ".hashCode" must {
      "return a hash integer for the iabdType rather than the BenefitInKindRequest instance" in {
        val generatedHash: Int        = IabdType.CarBenefit.id.hashCode
        val bik: BenefitInKindRequest = BenefitInKindRequest(
          IabdType.CarBenefit,
          PbikAction.ReinstatePayrolledBenefitInKind,
          isAgentSubmission = false
        )

        bik.hashCode mustBe generatedHash
      }
    }
  }
}
