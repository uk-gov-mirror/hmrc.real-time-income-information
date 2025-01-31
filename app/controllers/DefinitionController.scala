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

package controllers

import config.APIConfig
import javax.inject.{Inject, Singleton}
import models.api.APIAccess
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import views.txt

@Singleton
class DefinitionController @Inject() (apiContext: APIConfig, cc: ControllerComponents) extends BackendController(cc) {

  private val apiAccess: APIAccess = APIAccess(apiContext.apiTypeAccess, apiContext.apiWhiteList)

  def get(): Action[AnyContent] =
    Action {
      Ok(txt.definition(apiAccess, apiContext.apiContext))
        .as(contentType = "application/json")
        .withHeaders(CONTENT_TYPE -> JSON)
    }

}
