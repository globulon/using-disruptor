package com.promindis.disruptor.adapters

import java.util.concurrent.Executors._
import com.promindis.disruptor.port.RSequence

trait Processor extends Runnable{
  def halt()
  def getSequence: RSequence
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
