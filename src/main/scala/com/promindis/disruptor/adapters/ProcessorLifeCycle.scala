package com.promindis.disruptor.adapters

import com.lmax.disruptor.BatchEventProcessor
import java.util.concurrent.Executors._

trait Processor extends Runnable{def halt()}

object ProcessorLifeCycle {


  implicit def batchToProcessor[T](processor: BatchEventProcessor[T]) = new Processor {
    def run() {
      processor.run()
    }

    def halt() {
      processor.halt()
    }

  }

  def executing[Proc <% Processor](processors: Proc*)(block: => Unit) {
    val executor = newFixedThreadPool(processors.length)
    processors.foreach {executor.execute(_)}
    try {
      block
    } finally {
      processors.foreach{_.halt()}
      executor.shutdown()
    }
  }

}
