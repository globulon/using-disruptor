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
 * */
object TimeMeasurement {

  def measured(block:  => Unit) = {
    val start = currentTimeMillis
    block
    val end = currentTimeMillis
    new {
      def getting[T](f: Long => T) = { f(end - start)}
    }
  }
}