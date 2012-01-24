package com.promindis.disruptor.adaptaters

import System._

/**
 * Basically measures time and
 * and allows the immediate invocation of
 * a callback function using the elapsed time
 *
 * measured {
 *   doYourStuff()
 * } getting { elapsedTime =>
 *  doSomethingWith(elapsedTime)
 * }
 * No exception management from now
 **/
object TimeMeasurement {

  implicit def toFrequency(value: Long) = new {
    def throughput(during: Long): Long = {
      (value * 1000L) / during
    }
  }


  def sampling(block: => Unit) = {
    val start = currentTimeMillis
    block
    val end = currentTimeMillis
    new {
      def provided[T](f: Long => T) = {
        f(end - start)
      }
    }
  }
}