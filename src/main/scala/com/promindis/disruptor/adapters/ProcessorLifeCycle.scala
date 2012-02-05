package com.promindis.disruptor.adapters

import java.util.concurrent.Executors._
import com.lmax.disruptor.Sequence

trait Processor extends Runnable{
  def halt()
  def getSequence: Sequence
}

object ProcessorLifeCycle {

  def executing(processors: Processor*)(block: => Unit) {
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
