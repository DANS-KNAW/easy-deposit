/**
 * Copyright (C) 2015-2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy.sword2

import java.io.File
import java.util

import nl.knaw.dans.easy.sword2.DepositHandler._
import org.apache.abdera.i18n.iri.IRI
import org.swordapp.server._

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

class ContainerManagerImpl extends ContainerManager {
  this: ApplicationSettings =>

  @throws(classOf[SwordServerException])
  @throws(classOf[SwordError])
  @throws(classOf[SwordAuthException])
  override def getEntry(editIRI: String, accept: util.Map[String, String], auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    implicit val settings = config.asInstanceOf[SwordConfig].settings
    SwordID.extract(editIRI) match {
      case Success(id) =>
        val dir: File = new File(settings.depositRootDir, id)
        if (!dir.exists) {
          throw new SwordError(404)
        }
      case _ => throw new SwordError(404)
    }

    val dr: DepositReceipt = new DepositReceipt
    dr.setEditIRI(new IRI(editIRI))
    dr.setLocation(new IRI(editIRI))
    dr.setEditMediaIRI(new IRI(editIRI))
    dr.setPackaging(List("http://purl.org/net/sword/package/BagIt"))
    dr.setTreatment("Dataset is stored at DANS (http://dans.knaw.nl/).")
    dr
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def replaceMetadata(editIRI: String, deposit: Deposit, auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    throw new SwordError("http://purl.org/net/sword/error/MethodNotAllowed")
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def replaceMetadataAndMediaResource(editIRI: String, deposit: Deposit, auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    throw new SwordError("http://purl.org/net/sword/error/MethodNotAllowed")
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def addMetadataAndResources(editIRI: String, deposit: Deposit, auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    throw new SwordError("http://purl.org/net/sword/error/MethodNotAllowed")
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def addMetadata(editIRI: String, deposit: Deposit, auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    throw new SwordError("http://purl.org/net/sword/error/MethodNotAllowed")
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def addResources(editIRI: String, deposit: Deposit, auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    implicit val settings = config.asInstanceOf[SwordConfig].settings
    val result = for {
      _ <- Authentication.checkAuthentication(auth)
      id <- SwordID.extract(editIRI)
      _  <- settings.auth match {
          case _ :LdapAuthSettings => checkThatUserIsOwnerOfDeposit(id, auth.getUsername)
          case _ => Success(())
        }
      _ = log.debug(s"[$id] Continued deposit")
      _ <- checkDepositIsInDraft(id)
      depositReceipt <- handleDeposit(deposit)(settings, id)
    } yield (id, depositReceipt)

    result.getOrThrow
  }

  private def checkThatUserIsOwnerOfDeposit(id: String, user: String)(implicit settings: Settings): Try[Unit] = {
    for {
      props <- DepositProperties(id)
      depositor <- props.getDepositorId
      _ <- if (depositor == user) Success(())
           else Failure(new SwordAuthException("Not allowed to continue deposit for other user"))
    } yield ()
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def deleteContainer(editIRI: String, auth: AuthCredentials, config: SwordConfiguration) {
    throw new SwordError("http://purl.org/net/sword/error/MethodNotAllowed")
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  def useHeaders(editIRI: String, deposit: Deposit, auth: AuthCredentials, config: SwordConfiguration): DepositReceipt = {
    throw new SwordError("http://purl.org/net/sword/error/MethodNotAllowed")
  }

  @throws(classOf[SwordError])
  @throws(classOf[SwordServerException])
  @throws(classOf[SwordAuthException])
  override def isStatementRequest(editIRI: String, accept: util.Map[String, String], auth: AuthCredentials, config: SwordConfiguration): Boolean = {
    false
  }

}

