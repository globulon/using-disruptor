package com.promindis.disruptor.port

import java.util.logging.{Level, Logger}
import Level.SEVERE

final class FatalExceptionHandler extends ExceptionHandler{
  private val LOGGER: Logger = Logger.getLogger(classOf[FatalExceptionHandler].getName)

  override def handleEventException(ex: Throwable, sequence: Long, event: Any) = {
    LOGGER.log(SEVERE, "Exception processing: " + sequence + " " + event, ex)
  }

  override def handleOnStartException(ex: Throwable) =
    LOGGER.log(SEVERE, "Exception during onStart()", ex)

  override def handleOnShutdownException(ex: Throwable) =
    LOGGER.log(SEVERE, "Exception during onShutdown()", ex)
}

object FatalExceptionHandler{
  def apply()=  new FatalExceptionHandler
}