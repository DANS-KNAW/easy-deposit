/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.api.sword2

import nl.knaw.dans.api.sword2.DepositHandler._
import org.swordapp.server._
import org.apache.commons.lang.StringUtils._

import scala.util.{Try, Failure, Success}

class CollectionDepositManagerImpl extends CollectionDepositManager {
  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def createNew(collectionURI: String, deposit: Deposit, auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    implicit val settings = config.asInstanceOf[SwordConfig].settings
    val result = for {
      _ <- Authentication.checkAuthentication(auth)
      _ <- checkValidCollectionId(collectionURI)
      maybeSlug = if(isNotBlank(deposit.getSlug)) Some(deposit.getSlug) else None
      id <- SwordID.generate(maybeSlug, auth.getUsername)
      _ = log.info(s"[$id] Created new deposit")
      _ <- setDepositStateToDraft(id, auth.getUsername)
      depositReceipt <- handleDeposit(deposit)(settings, id)
    } yield (id, depositReceipt)

    result.getOrThrow
  }

  def checkValidCollectionId(iri: String)(implicit settings: Settings): Try[Unit] = Try {
    if(iri != settings.collectionIri) throw new SwordError("http://purl.org/net/sword/error/MethodNotAllowed", 405, s"Not a valid collection IRI: $iri")
  }

  private def setDepositStateToDraft(id: String, userId: String)(implicit settings: Settings): Try[Unit] =
    DepositProperties.set(
      id = id,
      stateLabel = "DRAFT",
      stateDescription = "Deposit is open for additional data",
      userId = Some(userId),
      lookInTempFirst = true)

}
