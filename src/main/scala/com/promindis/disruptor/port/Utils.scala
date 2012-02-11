package com.promindis.disruptor.port

/**
 * Date: 11/02/12
 * Time: 16:20
 */

object Utils {


  import System.currentTimeMillis

  case class TimeSlice(start: Long = currentTimeMillis(), current: Long = currentTimeMillis(), interval: Long) {

    def slice() = copy(current = currentTimeMillis())

    def overdue() = (current - start) > interval

  }


  @inline def smallestSlotIn(sequences: Seq[RSequence]): Long = {
    sequences.foldLeft(Long.MaxValue) {
      (cur, item) =>
        if (item.get() < cur) item.get() else cur}
  }

}
