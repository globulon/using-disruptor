package com.promindis.disruptor.port

import java.util.concurrent.CountDownLatch
import util.Random

final class EventHandlerLifeCycleAware[T]
(val latch: CountDownLatch,
 val expectedNumberOfEvents: Long,
 val fail: Boolean) extends LifeCycleAware[T]{

  val failureIndex = if (fail) new Random().nextInt(expectedNumberOfEvents.toInt) else -1
  println("Failure expected at : " + failureIndex)

  var wasStarted = false

  var wasStopped = false

  var count = 0

  def onEvent(event: T, sequence: Long, endOfBatch: Boolean)  {
    if (count == failureIndex) throw new RuntimeException()
    count += 1
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