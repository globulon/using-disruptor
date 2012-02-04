package com.promindis.disruptor.port

trait ExceptionHandler {
  def handleEventException(ex: Throwable, sequence: Long, event: Any)
//  def handleOnStartException(ex: Throwable)
//  def handleOnShutdownException(ex: Throwable)
}