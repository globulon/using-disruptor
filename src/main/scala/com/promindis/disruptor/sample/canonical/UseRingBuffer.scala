package com.promindis.disruptor.sample.canonical

import com.promindis.disruptor.adaptaters.EventModule
import com.promindis.disruptor.adaptaters.RingBufferFactory

/**
 * User: omadas
 * Date: 23/01/12
 * Time: 12:41
 */

object UseRingBuffer{
  import EventModule._
  import Publisher._

  def main(args: Array[String]) {
    val (ringBuffer, withEventProcessor)  = RingBufferFactory.create(ValueEventFactory , Handler("one"))

    val processing = ProcessingLifeCycle(withEventProcessor)

    publishTo(ringBuffer)

    processing !! 'stop

  }
}
