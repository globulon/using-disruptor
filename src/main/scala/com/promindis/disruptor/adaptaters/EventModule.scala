package com.promindis.disruptor.adaptaters

import com.lmax.disruptor._
import java.util.concurrent.CountDownLatch

/**
 * We need a value event and an event factory to
 * feed the ring buffer
 */


object EventModule {
  class ValueEvent() {
    var value: Long = _

    def setValue(newValue: Long) {
      value = newValue
    }

    def getValue = value
  }

  object ValueEvent {
    def apply() = new ValueEvent()
  }

  object ValueEventFactory extends EventFactory[ValueEvent]{
    def newInstance(): ValueEvent = ValueEvent()
  }

  case class Handler(name: String, expectedShoot: Long = 0, latch: Option[CountDownLatch] = None) extends EventHandler[ValueEvent] {
    var counter = 0L

    def onEvent(event: ValueEvent, sequence: Long, endOfBatch: Boolean) {
      counter += 1L
      for (l <- latch if (counter == expectedShoot) ) {
        l.countDown()
      }
    }

    override def toString = "[" + name   + ": counter => " + counter  + "]"
  }

  def fillEvent(event: ValueEvent): ValueEvent = {
    event.setValue(1234);
    event
  }


}




