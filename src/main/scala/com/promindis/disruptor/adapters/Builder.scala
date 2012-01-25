package com.promindis.disruptor.adapters

import com.promindis.disruptor.support.StateMonad
import com.lmax.disruptor.{BatchEventProcessor, RingBuffer, EventHandler}


object Builder {
  def pipe[T](handler: EventHandler[T], rb: RingBuffer[T]) = new StateMonad[Unit, Seq[(BatchEventProcessor[T], EventHandler[T])] ] {
    def apply(list: Seq[(BatchEventProcessor[T], EventHandler[T])]) = {
      list match {
        case (p::ps)=>
          val newProcessor = new BatchEventProcessor[T](rb, rb.newBarrier(p._1.getSequence), handler)
          rb.setGatingSequences(newProcessor.getSequence)
          ((), (newProcessor, handler)::p::ps)
        case _ =>
          val newProcessor = new BatchEventProcessor[T](rb, rb.newBarrier(), handler)
          rb.setGatingSequences(newProcessor.getSequence)
          ((), (newProcessor, handler)::Nil)
      }
    }
  }
}