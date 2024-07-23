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

package controllers.actions

import helper.FakePBIKApplication
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Credentials

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MinimalAuthActionSpec extends PlaySpec with FakePBIKApplication with Results {

  private class Harness(authAction: MinimalAuthAction) extends BaseController {
    def onPageLoad(): Action[AnyContent] = authAction(_ => Ok)

    override protected def controllerComponents: ControllerComponents =
      fakeApplication.injector.instanceOf[ControllerComponents]
  }

  "MinimalAuthAction" when {

    "the user is logged in" must {
      "return the request and has pid" in {
        val mockAuthConnector: AuthConnector = mock(classOf[AuthConnector])
        when(mockAuthConnector.authorise[Option[Credentials]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Option(Credentials("providerId", "providerType"))))

        val minimalAuthAction = new MinimalAuthActionImpl(
          authConnector = mockAuthConnector,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller        = new Harness(minimalAuthAction)
        val result            = controller.onPageLoad()(FakeRequest("", ""))

        status(result) mustBe OK
      }
    }

    "the user is logged in" must {
      "return the request but has not pid" in {
        val mockAuthConnector: AuthConnector = mock(classOf[AuthConnector])
        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val minimalAuthAction = new MinimalAuthActionImpl(
          authConnector = mockAuthConnector,
          parser = app.injector.instanceOf[BodyParsers.Default]
        )
        val controller        = new Harness(minimalAuthAction)
        val result            = controller.onPageLoad()(FakeRequest("", ""))

        status(result) mustBe OK
      }
    }

    "the user hasn't logged in" must {
      "redirect the user to UNAUTHORIZED " in {
        val result: Future[Result] = handleAuthError(MissingBearerToken())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "the user's session has expired" must {
      "redirect the user to UNAUTHORIZED " in {
        val result: Future[Result] = handleAuthError(BearerTokenExpired())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "the user's credentials are invalid" must {
      "redirect the user to UNAUTHORIZED " in {
        val result: Future[Result] = handleAuthError(InvalidBearerToken())

        status(result) mustBe UNAUTHORIZED
      }
    }

    "the user's session cannot be found" must {
      "redirect the user to UNAUTHORIZED " in {
        val result: Future[Result] = handleAuthError(SessionRecordNotFound())

        status(result) mustBe UNAUTHORIZED
      }
    }

  }

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private def handleAuthError(exc: AuthorisationException): Future[Result] = {
    val authAction = new MinimalAuthActionImpl(
      new FakeFailingAuthConnector(exc),
      fakeApplication.injector.instanceOf[BodyParsers.Default]
    )
    val controller = new Harness(authAction)
    controller.onPageLoad()(fakeRequest)
  }

}
