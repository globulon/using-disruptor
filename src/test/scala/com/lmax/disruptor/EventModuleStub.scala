package com.lmax.disruptor

import java.util.concurrent.CountDownLatch
import com.promindis.disruptor.port.PaddedLong

object EventModuleStub {

  class ValueEvent() {
    val paddedValue = PaddedLong()

    def setValue(newValue: Long) {
      paddedValue.value = newValue
    }

    def getValue = paddedValue
  }

  object ValueEvent {
    def apply() = new ValueEvent()
  }

  object ValueEventFactory extends com.lmax.disruptor.EventFactory[ValueEvent] {
    def newInstance(): ValueEvent = ValueEvent()
  }

  case class Handler(name: String, expectedShoot: Long = 0, latch: Option[CountDownLatch] = None) extends EventHandler[ValueEvent] {
    var counter = 0L

    override def onEvent(event: ValueEvent, sequence: Long, endOfBatch: Boolean)  {
      counter += 1L
      for (l <- latch if (counter == expectedShoot)) {
        l.countDown()
      }
      Some(sequence)
    }

    override def toString = "[" + name + ": counter => " + counter + "]"
  }

  def fillEvent(event: ValueEvent): ValueEvent = {
    event.setValue(1234);
    event
  }
}
