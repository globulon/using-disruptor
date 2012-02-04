package com.promindis.disruptor.port

trait ExceptionHandler {
  /**
   *
   * @param ex caught exception
   * @param sequence current sequence
   * @param event handled event
   * @return None processing should be stopped
   */
  def handleEventException(ex: Throwable, sequence: Long, event: Any): Option[Long]
}