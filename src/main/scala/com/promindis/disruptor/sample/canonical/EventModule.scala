package com.promindis.disruptor.sample.canonical

import com.lmax.disruptor._

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

  class Handler() extends EventHandler[ValueEvent] {
    def onEvent(ev: ValueEvent, sequence: Long, endOfBatch: Boolean) {
      println("Received event " + ev + "with sequence" + sequence + " - " + endOfBatch)
    }
  }

  object Handler{
    def apply() = new Handler()
  }
}




