package com.lmax.disruptor

import org.specs2.mutable.Specification

/**
 * Date: 14/02/12
 * Time: 20:22
 */

class BatchDescriptorTest extends Specification{

  implicit def onSteroid(batch: BatchDescriptor) = new {
    def withEnd(value: Long) = {
      batch.setEnd(value)
      batch
    }
  }


  "new descriptor with size" should {

    "have a default end" in {
      new BatchDescriptor(7).getEnd.should(beEqualTo(-1L))
    }

    "bind size properly " in{
      new BatchDescriptor(7).getSize.should(beEqualTo(7))
    }

    "set end value properly " in {
      new BatchDescriptor(5).withEnd(11L).getEnd.should(beEqualTo(11L))
    }

    "map start  properly" in {
      new BatchDescriptor(5).withEnd(11L).getStart.should(beEqualTo(7L))
    }
  }

}
