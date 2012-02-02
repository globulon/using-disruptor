package com.promindis.disruptor.adapters

import com.lmax.disruptor._
import java.util.concurrent.CountDownLatch
import util.PaddedLong

/**
 * We need a value event and an event factory to
 * feed the ring buffer
 */


object EventModule {
  class ValueEvent() {
    val value = new PaddedLong()

    def setValue(newValue: Long) {
      value.set(newValue)
    }

    def getValue = value.get()
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
//      println(counter)
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




