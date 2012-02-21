package com.promindis.disruptor.port

/**
 * Date: 11/02/12
 * Time: 16:20
 */

object Utils {


  import System.currentTimeMillis

  case class VanishingTime(start: Long = currentTimeMillis(), current: Long = currentTimeMillis(), interval: Long) {

    def reduce() = copy(current = currentTimeMillis())

    def overdue() = (current - start) > interval

  }


  @inline def smallestSlotIn(sequences: Seq[RSequence]): Long = sequences.min.get()

}
