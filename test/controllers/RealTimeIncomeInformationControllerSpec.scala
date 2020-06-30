/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import akka.stream.Materializer
import app.Constants
import models.RequestDetails
import models.response._
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Injecting}
import services.{AuditService, RealTimeIncomeInformationService}
import uk.gov.hmrc.auth.core.AuthConnector
import utils.{BaseSpec, FakeAuthConnector}

import scala.concurrent.Future

class RealTimeIncomeInformationControllerSpec extends BaseSpec with GuiceOneAppPerSuite with Injecting with BeforeAndAfterEach {

  val correlationId: String = generateUUId
  val nino: String = generateNino
  val mockRtiiService: RealTimeIncomeInformationService = mock[RealTimeIncomeInformationService]
  val mockAuditService: AuditService = mock[AuditService]
  implicit val mat: Materializer = app.materializer

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[RealTimeIncomeInformationService].toInstance(mockRtiiService),
        bind[AuditService].toInstance(mockAuditService),
        bind[AuthConnector].toInstance(FakeAuthConnector))
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRtiiService, mockAuditService)
  }

  def fakeRequest(jsonBody : JsValue): FakeRequest[JsValue] = {
    FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = jsonBody)
  }

  val controller: RealTimeIncomeInformationController = inject[RealTimeIncomeInformationController]
  "preSchemaValidation" should {
    "Return OK provided a valid request" when {
      "the service returns a successfully filtered response" in {

        val values = Json.toJson(Map(
          "surname" -> "Surname",
          "nationalInsuranceNumber" -> nino
        ))

        val requestDetails: RequestDetails = exampleDwpRequest.as[RequestDetails]

        val expectedDesResponse = DesFilteredSuccessResponse(63, List(values))
        when(mockAuditService.rtiiAudit(meq(correlationId), meq(requestDetails))(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(meq(requestDetails), meq(correlationId))(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)

        verify(mockAuditService, times(1)).rtiiAudit(meq(correlationId), meq(requestDetails))(any())

      }

      "the service returns a successful when match pattern is 0 and None is returned" in {
        val expectedDesResponse = DesSuccessResponse(0, None)
        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)

      }
    }

    "Return Bad Request" when {
      List(
        ("the nino is invalid", exampleDwpRequestInvalidNino, Constants.responseInvalidPayload),
        ("either fromDate or toDate is not defined in the request", exampleInvalidDatesNotDefined, Constants.responseInvalidPayload),
        ("a date is in the wrong format", exampleInvalidDateFormat, Constants.responseInvalidPayload),
        ("the toDate is equal to fromDate", exampleInvalidDatesEqualRequest, Constants.responseInvalidDatesEqual),
        ("the toDate is before fromDate", exampleInvalidDateRangeRequest, Constants.responseInvalidDateRange),
        ("the filter fields array contains an empty string field", exampleInvalidDwpEmptyStringField, Constants.responseInvalidPayload),
        ("the filter fields array contains duplicate fields", exampleInvalidDwpDuplicateFields, Constants.responseInvalidPayload),
        ("the filter fields array is empty", exampleInvalidDwpEmptyFieldsRequest, Constants.responseInvalidPayload),
        ("the request contains an unexpected filter field", exampleInvalidFilterFieldDwpRequest, Constants.responseInvalidPayload),
        ("the request contains an unexpected matching field", exampleInvalidMatchingFieldDwpRequest, Constants.responseInvalidPayload)
      ).foreach {
        case (testName, requestJson, expectedResponse) => testName in { //SCHEMA VALIDATION
          val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(requestJson))
          status(result) shouldBe BAD_REQUEST
          contentAsJson(result) shouldBe Json.toJson(expectedResponse)
        }
      }

      "the service returns a single error response" in {
        val expectedDesResponse = Constants.responseInvalidCorrelationId
        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "the service returns multiple error responses" in {
        val expectedDesResponse = DesMultipleFailureResponse(List(Constants.responseInvalidCorrelationId, Constants.responseInvalidDateRange))
        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "the correlationId is invalid" in {
        val result: Future[Result] = controller.preSchemaValidation("invalidCorrelationId")(fakeRequest(exampleDwpRequest))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(Constants.responseInvalidCorrelationId)
      }

      "the service layer returns a single failure response with invalid date range code" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeInvalidDateRange, "")
        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "the service layer returns a single failure response with invalid dates equal code" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeInvalidDatesEqual, "")

        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "the service layer returns a single failure response with invalid payload code" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeInvalidPayload, "")

        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }
    }
    //TODO finish test when Auth work complete?
    "Return 403 (FORBIDDEN)" when {
      "A non privileged application attempts to call the endpoint" in {
      }
    }

    "Return 404 (NOT_FOUND)" when {
      "The remote endpoint has indicated that there is no data for the Nino" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeNotFoundNino, "")

        when(mockAuditService.rtiiAudit(any(), any())(any()))
          .thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any()))
          .thenReturn(Future.successful(expectedDesResponse))

        val result: Future[Result] = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))
        status(result) shouldBe NOT_FOUND
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "The controller receives an Error Code Not Found Error from the service layer" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeNotFound, "")

        when(mockAuditService.rtiiAudit(any(), any())(any())).thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any())).thenReturn(expectedDesResponse)

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) shouldBe NOT_FOUND
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }
    }

    "Return 500 Internal Server Error" when {
      "The controller receives a DesUnexpectedResponse from the service layer" in {
        val expectedDesResponse = DesUnexpectedResponse()

        when(mockAuditService.rtiiAudit(any(), any())(any())).thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any())).thenReturn(expectedDesResponse)

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "The controller receives an Error Code Server Error from the service layer" in {
        val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeServerError, "")

        when(mockAuditService.rtiiAudit(any(), any())(any())).thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any())).thenReturn(expectedDesResponse)

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "The controller receives an unmatched DES error" in {
        val expectedDesResponse = DesSingleFailureResponse("", "")

        when(mockAuditService.rtiiAudit(any(), any())(any())).thenReturn(Future.successful(()))
        when(mockRtiiService.retrieveCitizenIncome(any(), any())(any())).thenReturn(expectedDesResponse)

        val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
      }

      "Service unavailable" when {
        "The controller receives a failure response from DES in the service layer" in {
          when(mockAuditService.rtiiAudit(any(), any())(any())).thenReturn(Future.successful(()))
          when(mockRtiiService.retrieveCitizenIncome(any(), any())(any())).thenReturn(Future.failed(new Exception))

          val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

          status(result) shouldBe SERVICE_UNAVAILABLE
          contentAsJson(result) shouldBe Json.toJson(DesSingleFailureResponse(Constants.errorCodeServiceUnavailable,
            "Dependent systems are currently not responding."))
        }

        "The controller receives Des single failure response service unavailable" in {
          val expectedDesResponse = DesSingleFailureResponse(Constants.errorCodeServiceUnavailable, "")

          when(mockAuditService.rtiiAudit(any(), any())(any())).thenReturn(Future.successful(()))
          when(mockRtiiService.retrieveCitizenIncome(any(), any())(any())).thenReturn(Future.successful(expectedDesResponse))

          val result = controller.preSchemaValidation(correlationId)(fakeRequest(exampleDwpRequest))

          status(result) shouldBe SERVICE_UNAVAILABLE
          contentAsJson(result) shouldBe Json.toJson(expectedDesResponse)
        }
      }
    }
  }
}
