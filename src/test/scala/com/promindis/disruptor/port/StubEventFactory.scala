package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicLong

/**
 * Date: 18/02/12
 * Time: 19:24
 */

final case class StubEvent(data: Long)

final class StubEventFactory extends EventFactory[StubEvent]{
  val counter = new AtomicLong(0L)
  def apply() = StubEvent(counter.getAndIncrement)
}

object StubEventFactory {
  def apply () = new StubEventFactory
}
