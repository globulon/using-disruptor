package com.promindis.disruptor.port

trait EventHandler[T] {
  def onEvent(event: T, sequence: Long, endOfBatch: Boolean): Option[Long]
}

trait LifeCycleAware[T] extends EventHandler[T]{
  def started();

  def stopped();

}