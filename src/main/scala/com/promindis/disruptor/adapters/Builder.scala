package com.promindis.disruptor.adapters

import com.promindis.disruptor.support.StateMonad

import com.lmax.disruptor.{SequenceBarrier, RingBuffer}
import com.promindis.disruptor.port.EventHandler


object Builder {
  type Context[X] = (Processor, EventHandler[X])

  def fork[T](handler: EventHandler[T], rb: RingBuffer[T], currentBarrier: SequenceBarrier)(implicit processorFactory: ProcessorFactory) = new StateMonad[SequenceBarrier, List[(Context[T])]] {
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

  def pipe[T](handler: EventHandler[T], rb: RingBuffer[T])(implicit processorFactory: ProcessorFactory) = new StateMonad[SequenceBarrier, List[(Context[T])]]{
    def apply(list: List[Context[T]]) = {
      list match {
        case (p::ps)=>
          val newBarrier = rb.newBarrier(p._1.getSequence)
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newBarrier = rb.newBarrier()
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }

  def join[T](handler: EventHandler[T], rb: RingBuffer[T])(implicit processorFactory: ProcessorFactory) = new StateMonad[SequenceBarrier, List[(Context[T])]]{
    def apply(list: List[Context[T]]) = {
      list match {
        case (p::ps)=>
          val sequences = list.unzip._1.map{_.getSequence}
          val newBarrier = rb.newBarrier(sequences: _*)
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newBarrier = rb.newBarrier()
          val newProcessor = processorFactory.create(rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }
}