package com.promindis.disruptor.adapters

import com.lmax.disruptor.BatchEventProcessor
import java.util.concurrent.Executors._

object ProcessorLifeCycle {

  def executing[T](processors: BatchEventProcessor[T]*)(block: => Unit) {
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
