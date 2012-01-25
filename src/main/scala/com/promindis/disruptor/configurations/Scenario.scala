package com.promindis.disruptor.configurations

import com.lmax.disruptor.BatchEventProcessor
import com.promindis.disruptor.adapters.TimeMeasurement._
import com.promindis.disruptor.adapters.ProcessorLifeCycle._


final case class Configuration(
  ringBufferSize: Int = 1024 * 1024,
  iterations: Long = 1000L * 1000L * 10L,
  runs: Int  = 5
)

trait Scenario {

  final def playWith[T](processors: List[BatchEventProcessor[T]])(scenario: => Unit)(implicit config: Configuration) = {
    sampling {
      executing(processors:_*) {
        scenario
      }
    } provided {
      config.iterations.throughput(_)
    }
  }

  def challenge(implicit configuration: Configuration): Long

  def run(implicit config: Configuration): Seq[Long] =  {
    val config = Configuration()
    for (_ <- 1 to config.runs) yield challenge(config)
  }

  def main(args: Array[String]) {
    run(Configuration())
      .foreach{value => println("Nb Op/s: " + value)}
  }
}