package com.promindis.disruptor

import com.promindis.disruptor.EventModule.{ValueEventFactory, ValueEvent}
import com.lmax.disruptor._
import java.util.concurrent.Executors._
import actors.Actor



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

  class Handler(val ringLifeCycle: RingLifeCycle) extends EventHandler[ValueEvent] {
    def onEvent(ev: ValueEvent, sequence: Long, endOfBatch: Boolean) {
      println("Received event " + ev + "with sequence" + sequence + " - " + endOfBatch)
      ringLifeCycle ! 'stop
    }
  }

  object Handler{
    def apply(ringLifeCycle: RingLifeCycle) = new Handler(ringLifeCycle)
  }
}


class RingLifeCycle(val ringBuffer: RingBuffer[ValueEvent]) extends Actor {
  self =>
  val executor = newSingleThreadExecutor()
  var eventProcessor: BatchEventProcessor[ValueEvent] = _

  def act {
    react {
      case 'start =>
        import EventModule._
        println("Starting...")
        val barrier = ringBuffer.newBarrier();
        eventProcessor = new BatchEventProcessor[ValueEvent](ringBuffer, barrier, Handler(self))
        ringBuffer.setGatingSequences(eventProcessor.getSequence);
        executor.submit(eventProcessor)
        reply("started")
        act
      case 'stop =>
        println("Stopping...")
        eventProcessor.halt()
        executor.shutdown()
        println("Exiting...")
        self.exit()
      case _ =>
        println("Trapped...")
        act
    }
  }
}

object RingLifeCycle {
  def apply(ringBuffer: RingBuffer[ValueEvent]) = {
    val manager = new RingLifeCycle(ringBuffer)
    manager.start()
    manager
  }
}

object UsRingBuffer{

  def ringBuffer =
    new RingBuffer[ValueEvent](
      ValueEventFactory,
      new SingleThreadedClaimStrategy(1024),
      new SleepingWaitStrategy());


  def nextEvent(rb: RingBuffer[EventModule.ValueEvent]): (Long, ValueEvent) = {
    val sequence = rb.next();
    (sequence, rb.get(sequence));
  }

  def prepare(event: EventModule.ValueEvent)  {
    event.setValue(1234);
  }

  def startLifeCycle(rb: RingBuffer[EventModule.ValueEvent]) = {
    (RingLifeCycle(rb) !! 'start)()
    rb
  }

  def main(args: Array[String]) {
    val rb = startLifeCycle(ringBuffer)

    val (sequence, event) = nextEvent(rb)
    prepare(event)
    rb.publish(sequence);

  }
}