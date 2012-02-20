package com.promindis.disruptor.adapters

import com.promindis.disruptor.port.{BatchEventProcessor, EventHandler, RingBuffer, SequencesBarrier}

trait ProcessorFactory {
  def create[T](rb: RingBuffer[T], barrier: SequencesBarrier, handler: EventHandler[T]): Processor
}

object ProcessorFactory {
  def apply() = new ProcessorFactory {
    def create[T](rb: RingBuffer[T], barrier: SequencesBarrier, handler: EventHandler[T]) = {
      BatchEventProcessor[T](rb, barrier, handler)
    }
  }

}

