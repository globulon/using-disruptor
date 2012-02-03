package com.promindis.disruptor.port

trait EventHandler[T] {
  def onEvent(event: T, sequence: Long, endOfBatch: Boolean)
}