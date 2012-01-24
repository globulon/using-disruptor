package com.promindis.disruptor.adapters

import com.lmax.disruptor._


/**
 * All the code required to create and use a ring buffer
 * is located here
 * I voluntary did not use the disruptor to go one level deeper
 * i the understanding
 * The imports are all from LMAX so I guess goal is reached
 */

object RingBufferFactory {
  val DEFAULT_SIZE: Int = 1024


  def ringBuffer[T](eventFactory: EventFactory[T], size: Int = DEFAULT_SIZE, waitStrategy: WaitStrategy = new SleepingWaitStrategy()) =
    new RingBuffer[T](eventFactory, new SingleThreadedClaimStrategy(size), waitStrategy);

  def create[T](withEventFactory: EventFactory[T], handler: EventHandler[T]) = {
    val rb = ringBuffer[T](withEventFactory)
    val barrier = rb.newBarrier();
    val eventProcessor = new BatchEventProcessor[T](rb, barrier, handler)
    rb.setGatingSequences(eventProcessor.getSequence);
    (rb, eventProcessor)
  }
}
