package com.promindis.disruptor.port

import java.util.logging.{Level, Logger}
import Level.SEVERE

final class FatalExceptionHandler extends ExceptionHandler{
  private val LOGGER: Logger = Logger.getLogger(classOf[FatalExceptionHandler].getName)

  override def handleEventException(ex: Throwable, sequence: Long, event: Any): Option[Long] =  {
    LOGGER.log(SEVERE, "Exception processing: " + sequence + " " + event, ex)
    None
  }

}

object FatalExceptionHandler{
  def apply()=  new FatalExceptionHandler
}