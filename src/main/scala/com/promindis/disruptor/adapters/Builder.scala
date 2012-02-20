package com.promindis.disruptor.adapters

import com.promindis.disruptor.support.StateMonad
import com.promindis.disruptor.port.{SequencesBarrier, RingBuffer, EventHandler}


object Builder {
  type Context[X] = (Processor, EventHandler[X])

  def fork[T](handler: EventHandler[T], rb: RingBuffer[T], currentBarrier: SequencesBarrier)(implicit processorFactory: ProcessorFactory) = new StateMonad[SequencesBarrier, List[(Context[T])]] {
    def apply(list: List[Context[T]]) = {
      list match {
        case (p::ps)=>
          val newProcessor = processorFactory.create(rb, currentBarrier, handler)
          (currentBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newProcessor = processorFactory.create(rb, currentBarrier, handler)
          (currentBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }

  def pipe[T](handler: EventHandler[T], rb: RingBuffer[T])(implicit processorFactory: ProcessorFactory) = new StateMonad[SequencesBarrier, List[(Context[T])]]{
    def apply(list: List[Context[T]]) = {
      list match {
        case (p::ps)=>
          val newBarrier = rb.barrierFor(p._1.getSequence)
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newBarrier = rb.barrier
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }

  def join[T](handler: EventHandler[T], rb: RingBuffer[T])(implicit processorFactory: ProcessorFactory) = new StateMonad[SequencesBarrier, List[(Context[T])]]{
    def apply(list: List[Context[T]]) = {
      list match {
        case (p::ps)=>
          val sequences = list.unzip._1.map{_.getSequence}
          val newBarrier = rb.barrierFor(sequences: _*)
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newBarrier = rb.barrier
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }
}