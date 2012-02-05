package com.promindis.disruptor.port

import java.util.concurrent.CountDownLatch
import util.Random

final class EventHandlerLifeCycleAware[T]
(val latch: CountDownLatch,
 val expectedNumberOfEvents: Long,
 val fail: Boolean) extends LifeCycleAware[T]{

  val failureIndex =
    if (fail)
      new Random().nextInt(expectedNumberOfEvents.toInt) + 1
    else -1

  var wasStarted = false
  var wasStopped = false

  var handledEvents = 0
  var receivedEvents = 0

  def onEvent(event: T, sequence: Long, endOfBatch: Boolean)  = {
    receivedEvents += 1
    if (sequence == failureIndex) throw new RuntimeException()
    handledEvents += 1
    Some(sequence)
  }

  def started() { wasStarted = true}

  def stopped() {
    wasStopped = true
    latch.countDown()
  }
}

object EventHandlerLifeCycleAware{
  def apply[T](latch: CountDownLatch, expectedNumberOfEvents: Long, fail: Boolean = false) =
    new EventHandlerLifeCycleAware[T](latch, expectedNumberOfEvents, fail)
}