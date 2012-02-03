package com.promindis.disruptor.configurations

import com.promindis.disruptor.adapters.TimeMeasurement._
import com.promindis.disruptor.adapters.Processor
import com.promindis.disruptor.adapters.ProcessorLifeCycle._

final case class Configuration(
  ringBufferSize: Int = 1024 * 1024,
  iterations: Long = 1000L * 1000L * 48L,
  runs: Int  = 5
)

trait Scenario {

  final def playWith[Proc](processors: Seq[Proc])(bench: => Unit)
                          (implicit config: Configuration, c: Proc => Processor) = {
    sampling {
      executing(processors:_*) {
        bench
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