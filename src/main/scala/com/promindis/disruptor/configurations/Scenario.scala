package com.promindis.disruptor.configurations

import com.promindis.disruptor.adapters.TimeMeasurement._
import com.promindis.disruptor.adapters.ProcessorLifeCycle._
import com.promindis.disruptor.adapters.{ProcessorFactory, Processor}


final case class Configuration(
  ringBufferSize: Int = 1024 * 128,
  iterations: Long = 1024L * 1024L * 64,
  runs: Int  = 8
)

trait Scenario {

  implicit val factory = ProcessorFactory()

  final def playWith(processors: Seq[Processor])(bench: => Unit) (implicit config: Configuration) = {
    sampling {
      executing(processors:_*) {
        bench
      }
    } provided {
      config.iterations.throughput(_)
    }
  }

  def challenge(implicit configuration: Configuration, factory: ProcessorFactory): Long

  def run(implicit config: Configuration, factory: ProcessorFactory): Seq[Long] =  {
    val config = Configuration()
    for (_ <- 1 to config.runs) yield challenge(config, factory)
  }

  def main(args: Array[String]) {
    val result = run(Configuration(), factory).foldLeft(0L) { _ + _ }
    println("Nb Op/s: " + result / Configuration().runs)
  }
}