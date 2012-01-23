package com.promindis.disruptor

import com.promindis.disruptor.EventModule.{ValueEventFactory, ValueEvent}
import com.lmax.disruptor._
import java.util.concurrent.Executors._


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
}


object Handler extends EventHandler[ValueEvent] {
  def onEvent(ev: ValueEvent, sequence: Long, endOfBatch: Boolean) {
    println("Received event " + ev + "with sequence" + sequence + " - " + endOfBatch)
  }
}


object UseRing{

  val executor = newSingleThreadExecutor()

  def main(args: Array[String]) {
    val ringBuffer =
      new RingBuffer[ValueEvent](
        ValueEventFactory,
        new SingleThreadedClaimStrategy(1024),
        new SleepingWaitStrategy());

    val barrier = ringBuffer.newBarrier();
    val eventProcessor = new BatchEventProcessor[ValueEvent](ringBuffer, barrier, Handler)

    executor.submit(eventProcessor)
    ringBuffer.setGatingSequences(eventProcessor.getSequence);

    // Publishers claim events in sequence
    val sequence = ringBuffer.next();
    val event = ringBuffer.get(sequence);

    // this could be more complex with multiple fields
    event.setValue(1234);
    // make the event available to EventProcessors
    ringBuffer.publish(sequence);
    println("waiting")
//     Handler.latch.await();
    executor.shutdownNow()

  }
}