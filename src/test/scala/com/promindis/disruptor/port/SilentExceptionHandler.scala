package com.promindis.disruptor.port

trait SilentExceptionHandler extends ExceptionHandler{
  var caughtSomeExceptionHandler = false
  def caughtSomeExceptionHandler_ = caughtSomeExceptionHandler

  def statusFor(l: Long): Option[Long]

  override def handleEventException(ex: Throwable, sequence: Long, event: Any)= {
    caughtSomeExceptionHandler = true
    statusFor(sequence)
  }

  def handleOnShutdownException(ex: Throwable) = null

  def handleOnStartException(ex: Throwable) = null
}

object SilentExceptionHandler {
  def fatal() = new SilentExceptionHandler {
    def statusFor(sequence: Long) = None
  }

  def tolerant() = new SilentExceptionHandler {
    def statusFor(sequence: Long) = Some(sequence)
  }
}
