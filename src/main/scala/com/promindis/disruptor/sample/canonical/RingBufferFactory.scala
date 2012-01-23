package com.promindis.disruptor.sample.canonical

import com.lmax.disruptor._

/**
 * User: omadas
 * Date: 23/01/12
 * Time: 12:32
 */

object RingBufferFactory {
  def ringBuffer[T](eventFactory: EventFactory[T]) =
    new RingBuffer[T](
      eventFactory,
      new SingleThreadedClaimStrategy(1024),
      new SleepingWaitStrategy());


  def create[T](withEventFactory: EventFactory[T], handler: EventHandler[T]) = {
    val rb = ringBuffer[T](withEventFactory)
    val barrier = rb.newBarrier();
    val eventProcessor = new BatchEventProcessor[T](rb, barrier, handler)
    rb.setGatingSequences(eventProcessor.getSequence);
    (rb, eventProcessor)
  }
}
