package com.promindis.disruptor.adapters

import com.lmax.disruptor.{SequenceBarrier, RingBuffer}
import com.promindis.disruptor.port.{BatchEventProcessor, EventHandler}

trait ProcessorFactory {
  def create[T](rb: RingBuffer[T], barrier: SequenceBarrier, handler: EventHandler[T]): Processor
}

object ProcessorFactory {
  def apply() = new ProcessorFactory {
    def create[T](rb: RingBuffer[T], barrier: SequenceBarrier, handler: EventHandler[T]) = {
      BatchEventProcessor[T](rb, barrier, handler)
    }
  }

}

