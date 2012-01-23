package com.promindis.disruptor.sample.canonical

import com.lmax.disruptor.RingBuffer
import EventModule.ValueEvent
import actors.Actor

/**
 * User: omadas
 * Date: 23/01/12
 * Time: 12:51
 */

class Publisher(val ringBuffer: RingBuffer[ValueEvent]) extends Actor {
  self =>
  def nextEventStructure(rb: RingBuffer[ValueEvent]): (Long, ValueEvent) = {
    val sequence = rb.next();
    (sequence, rb.get(sequence));
  }

  def prepare(event: ValueEvent)  {
    event.setValue(1234);
  }

  def act() {
    react {
      case 'publish =>
        println("start publishing...")
        val (sequence, event) = nextEventStructure(ringBuffer)
        prepare(event)
        ringBuffer.publish(sequence);
        self.reply('done)
      case 'stop =>
        println("stop publishing...")
        self.exit()
      case _ =>
        println("publisher trapped...")
        self.exit("Invalid order")
    }
  }
}

object Publisher {
  def publishTo(buffer: RingBuffer[ValueEvent]) {
    val publisher = Publisher(buffer)
    (publisher !!  'publish)()
    publisher ! 'stop
  }

  def apply(buffer: RingBuffer[ValueEvent]) = {
    val publisher = new Publisher(buffer)
    publisher.start()
    publisher
  }

}