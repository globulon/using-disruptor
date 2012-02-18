package com.lmax.disruptor

import java.util.concurrent.atomic.AtomicLong

/**
 * Date: 18/02/12
 * Time: 18:41
 */

case class StubEvent(data: Long)

final class StubEventFactory() extends EventFactory[StubEvent] {
  val counter = new AtomicLong(0)

  def newInstance() = StubEvent(counter.getAndIncrement)
}

object StubEventFactory {
  def apply() = new StubEventFactory
}
