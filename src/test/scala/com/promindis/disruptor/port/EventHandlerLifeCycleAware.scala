package com.promindis.disruptor.port

import java.util.concurrent.CountDownLatch

class EventHandlerLifeCycleAware[T](val latch: CountDownLatch, val expectedNumberOfEvents: Long) extends LifeCycleAware[T]{
  var wasStarted = false

  var wasStopped = false

  var count = 0

  def onEvent(event: T, sequence: Long, endOfBatch: Boolean)  {
    count += 1
  }

  def started() { wasStarted = true}

  def stopped() {
    wasStopped = false
    latch.countDown()
  }
}

object EventHandlerLifeCycleAware{
  def apply[T](latch: CountDownLatch, expectedNumberOfEvents: Long) = new EventHandlerLifeCycleAware[T](latch, expectedNumberOfEvents)
}