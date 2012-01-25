package com.promindis.disruptor.adapters

import com.promindis.disruptor.support.StateMonad
import com.lmax.disruptor.{SequenceBarrier, BatchEventProcessor, RingBuffer, EventHandler}


object Builder {

  def fork[T](handler: EventHandler[T], rb: RingBuffer[T], currentBarrier: SequenceBarrier) = new StateMonad[SequenceBarrier, List[(BatchEventProcessor[T], EventHandler[T])] ] {
    def apply(list: List[(BatchEventProcessor[T], EventHandler[T])]) = {
      list match {
        case (p::ps)=>
          val newProcessor = new BatchEventProcessor[T](rb, currentBarrier, handler)
          (currentBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newProcessor = new BatchEventProcessor[T](rb, currentBarrier, handler)
          (currentBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }


  def pipe[T](handler: EventHandler[T], rb: RingBuffer[T]) = new StateMonad[SequenceBarrier, List[(BatchEventProcessor[T], EventHandler[T])] ] {
    def apply(list: List[(BatchEventProcessor[T], EventHandler[T])]) = {
      list match {
        case (p::ps)=>
          val newBarrier = rb.newBarrier(p._1.getSequence)
          val newProcessor = new BatchEventProcessor[T](rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newBarrier = rb.newBarrier()
          val newProcessor = new BatchEventProcessor[T](rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }


  def join[T](handler: EventHandler[T], rb: RingBuffer[T]) = new StateMonad[SequenceBarrier, List[(BatchEventProcessor[T], EventHandler[T])] ] {
    def apply(list: List[(BatchEventProcessor[T], EventHandler[T])]) = {
      list match {
        case (p::ps)=>
          val sequences = list.unzip._1.map{_.getSequence}
          val newBarrier = rb.newBarrier(sequences: _*)
          val newProcessor = new BatchEventProcessor[T](rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler)::p::ps)
        case _ =>
          val newBarrier = rb.newBarrier()
          val newProcessor = new BatchEventProcessor[T](rb, newBarrier, handler)
          (newBarrier, (newProcessor, handler) :: Nil)
      }
    }
  }
}