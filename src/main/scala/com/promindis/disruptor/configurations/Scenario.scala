package com.promindis.disruptor.configurations

import com.lmax.disruptor.BatchEventProcessor
import com.promindis.disruptor.adapters.TimeMeasurement._
import com.promindis.disruptor.adapters.ProcessorLifeCycle._


trait Scenario {
  val ITERATIONS: Long


  final def play[T](scenario: => Unit)(processors: BatchEventProcessor[T]*) = {
    sampling {
      executing(processors:_*) {
        scenario
      }
    } provided {
      ITERATIONS.throughput(_)
    }
  }

}