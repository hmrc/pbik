/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import play.api.libs.json.{Json, OFormat}

case class EiLPerson(
  nino: String,
  firstForename: String,
  secondForename: Option[String],
  surname: String,
  worksPayrollNumber: Option[String],
  dateOfBirth: Option[String],
  gender: Option[String],
  status: Option[Int],
  perOptLock: Int = 0) {

  override def equals(obj: Any): Boolean = {

    val other = obj.asInstanceOf[EiLPerson]
    obj.isInstanceOf[EiLPerson] && (this.nino == other.nino)
  }

  override def hashCode: Int = nino.hashCode

}

object EiLPerson {

  val defaultStringArgumentValue: String = ""
  val defaultIntArgumentValue: Int = -1
  val defaultNino: String = defaultStringArgumentValue
  val defaultFirstName: String = defaultStringArgumentValue
  val defaultSecondName: Option[String] = Some(defaultStringArgumentValue)
  val defaultSurname: String = defaultStringArgumentValue
  val defaultWorksPayrollNumber: Option[String] = Some(defaultStringArgumentValue)
  val defaultDateOfBirth: Option[Nothing] = None
  val defaultGender: Option[String] = Some(defaultStringArgumentValue)
  val defaultStatus: Option[Int] = Some(defaultIntArgumentValue)
  val defaultPerOptLock: Int = defaultIntArgumentValue

  def secondaryComparison(x: EiLPerson, y: EiLPerson): Boolean =
    x.firstForename == y.firstForename &&
      x.surname == y.surname &&
      x.dateOfBirth.getOrElse("") == y.dateOfBirth.getOrElse("") &&
      x.gender.getOrElse("") == y.gender.getOrElse("")

  def defaultEiLPerson(): EiLPerson =
    EiLPerson(
      defaultNino,
      defaultFirstName,
      defaultSecondName,
      defaultSurname,
      defaultWorksPayrollNumber,
      defaultDateOfBirth,
      defaultGender,
      defaultStatus,
      defaultPerOptLock
    )

  implicit val eiLPersonFormat: OFormat[EiLPerson] = Json.format[EiLPerson]
}
