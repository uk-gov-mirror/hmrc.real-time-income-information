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

import config.{APIAccessConfig, ApiContext}
import javax.inject.{Inject, Singleton}
import models.api.APIAccess
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import views.txt

import scala.concurrent.Future

@Singleton
class DefinitionController @Inject()(appContext: ApiContext,
                                     cc: ControllerComponents) extends BackendController(cc) {
//TODO inject action, dont use action object?
  def get(): Action[AnyContent] = Action.async {
    Future.successful(Ok(txt.definition(buildAccess(), appContext.apiContext)).as("application/json").withHeaders(CONTENT_TYPE -> JSON))
  }

  private def buildAccess(): APIAccess = { //TODO should this be in appcontext?
    val access = APIAccessConfig(appContext.apiAccess)
    APIAccess(access.accessType, access.whiteListedApplicationIds)
  }

}
