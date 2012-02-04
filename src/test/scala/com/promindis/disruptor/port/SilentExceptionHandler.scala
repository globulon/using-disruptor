package com.promindis.disruptor.port

case class SilentExceptionHandler() extends ExceptionHandler{

  var caughtSomeExceptionHandler = false
  def caughtSomeExceptionHandler_ = caughtSomeExceptionHandler

  override def handleEventException(ex: Throwable, sequence: Long, event: Any) {
    caughtSomeExceptionHandler = true
  }

  def handleOnShutdownException(ex: Throwable) = null

  def handleOnStartException(ex: Throwable) = null
}
