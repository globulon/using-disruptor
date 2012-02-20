package com.promindis.disruptor.adapters

import java.util.concurrent.CountDownLatch
import com.promindis.disruptor.port.{PaddedLong, EventHandler, EventFactory}

/**
 * We need a value event and an event factory to
 * feed the ring buffer
 */


object EventModule {
  final class ValueEvent(var value: Long = 0L)
  object ValueEvent {
    def apply() = new ValueEvent()
  }

  object ValueEventFactory extends EventFactory[ValueEvent]{
    def apply(): ValueEvent = ValueEvent()
  }

  final class Handler(name: String, expectedShoot: Long = 0, latch: Option[CountDownLatch] = None) extends EventHandler[ValueEvent]{
    var counter = 0L

    override def onEvent(event: ValueEvent, sequence: Long, endOfBatch: Boolean) = {
      counter += 1L
      if (counter == expectedShoot) {
        for (l <- latch)
          l.countDown()
      }
      Some(sequence)
    }

    override def toString = "[" + name   + ": counter => " + counter  + "]"
  }

  object Handler {
    def apply(name: String, expectedShoot: Long = 0, latch: Option[CountDownLatch] = None): EventHandler[ValueEvent] =
      new Handler(name, expectedShoot, latch)
  }

  def fillEvent(event: ValueEvent): ValueEvent = {
    event.value = 1234L;
    event
  }

}




