/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.sword2

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime

import nl.knaw.dans.easy.sword2.DepositProperties.{ CLIENT_MESSAGE_CONTENT_TYPE_KEY, CLIENT_MESSAGE_CONTENT_TYPE_KEY_OLD, FILENAME }
import nl.knaw.dans.easy.sword2.State.State
import nl.knaw.dans.easy.sword2.{ DepositId, DepositProperties, Settings, State, dateTimeFormatter }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.lang.StringUtils
import org.joda.time.{ DateTime, DateTimeZone }

import scala.util.{ Failure, Success, Try }

class DepositPropertiesFile(depositId: DepositId, depositorId: Option[String] = None)(implicit settings: Settings) extends DepositProperties with DebugEnhancedLogging {

  trace(depositId, depositorId)

  private val (properties, modified) = {
    val props = new PropertiesConfiguration()
    props.setDelimiterParsingDisabled(true)
    val depositInTemp = settings.tempDir.toPath.resolve(depositId)
    val depositInInbox = settings.depositRootDir.toPath.resolve(depositId)
    val file = if (Files.exists(depositInTemp)) depositInTemp.resolve(FILENAME)
               else if (Files.exists(depositInInbox)) depositInInbox.resolve(FILENAME)
               else depositInTemp.resolve(FILENAME)
    props.setFile(file.toFile)
    if (Files.exists(file)) props.load(file.toFile)
    else {
      props.setProperty("bag-store.bag-id", depositId)
      props.setProperty("creation.timestamp", DateTime.now(DateTimeZone.UTC).toString(dateTimeFormatter))
      props.setProperty("deposit.origin", "SWORD2")
    }
    debug(s"Using deposit.properties at $file")
    depositorId.foreach(props.setProperty("depositor.userId", _))
    (props, if (Files.exists(file)) Some(Files.getLastModifiedTime(file))
            else None)
  }

  /**
   * Saves the deposit file to disk.
   *
   * @return
   */
  def save(): Try[Unit] = Try {
    debug("Saving deposit.properties")
    properties.save()
  }

  def exists: Boolean = properties.getFile.exists

  def setState(state: State, descr: String): Try[DepositProperties] = Try {
    properties.setProperty("state.label", state)
    properties.setProperty("state.description", descr)
    this
  }

  def setBagName(bagDir: File): Try[DepositProperties] = Try {
    properties.setProperty("bag-store.bag-name", bagDir.getName)
    this
  }

  /**
   * Returns the state when the properties were loaded.
   *
   * @return
   */
  def getState: Try[State] = {
    Option(properties.getProperty("state.label"))
      .map(_.toString)
      .map(State.withName)
      .map(Success(_))
      .getOrElse(Failure(new IllegalStateException("Deposit without state")))
  }

  def setClientMessageContentType(contentType: String): Try[DepositProperties] = Try {
    properties.setProperty(CLIENT_MESSAGE_CONTENT_TYPE_KEY, contentType)
    this
  }

  def removeClientMessageContentType(): Try[DepositProperties] = Try {
    properties.clearProperty(CLIENT_MESSAGE_CONTENT_TYPE_KEY)
    properties.clearProperty(CLIENT_MESSAGE_CONTENT_TYPE_KEY_OLD) // Also clean up old contentType property if still found
    this
  }

  def getClientMessageContentType: Try[String] = {
    Seq(properties.getString(CLIENT_MESSAGE_CONTENT_TYPE_KEY),
      properties.getString(CLIENT_MESSAGE_CONTENT_TYPE_KEY_OLD)) // Also look for old contentType to support pre-upgrade deposits
      .find(StringUtils.isNotBlank)
      .map(Success(_))
      .getOrElse(
        Failure(new IllegalStateException(s"Deposit without $CLIENT_MESSAGE_CONTENT_TYPE_KEY")))
  }

  /**
   * Returns the state description when the properties were loaded.
   *
   * @return
   */
  def getStateDescription: Try[String] = {
    Option(properties.getProperty("state.description"))
      .map(_.toString)
      .map(Success(_))
      .getOrElse(Failure(new IllegalStateException("Deposit without state")))
  }

  def getDepositorId: Try[String] = {
    Option(properties.getProperty("depositor.userId"))
      .map(_.toString)
      .map(Success(_))
      .getOrElse(Failure(new IllegalStateException("Deposit without depositor")))
  }

  def getDoi: Option[String] = {
    Option(properties.getProperty("identifier.doi"))
      .map(_.toString)
  }

  /**
   * Returns the last modified timestamp when the properties were loaded.
   *
   * @return
   */
  def getLastModifiedTimestamp: Option[FileTime] = {
    modified
  }
}