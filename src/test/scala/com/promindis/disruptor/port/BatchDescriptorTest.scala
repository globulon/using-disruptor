package com.promindis.disruptor.port

import org.specs2.mutable.Specification

/**
 * Date: 14/02/12
 * Time: 20:22
 */

class BatchDescriptorTest extends Specification {

   "new descriptor with size" should {

     "have a default end" in {
       new BatchDescriptor(7).end.should(beEqualTo(-1L))
     }

     "bind size properly " in{
       new BatchDescriptor(7).size.should(beEqualTo(7))
     }

     "set end value properly " in {
       new BatchDescriptor(5).withEnd(11L).end.should(beEqualTo(11L))
     }

     "map start  properly" in {
       new BatchDescriptor(5).withEnd(11L).start.should(beEqualTo(7L))
     }
   }

 }
