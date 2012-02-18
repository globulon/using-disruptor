package com.lmax.disruptor

import org.specs2.mutable.Specification
import collection.immutable.NumericRange

/**
 * Date: 18/02/12
 * Time: 18:33
 */

final class RingBufferTest extends Specification{
  def buffer = new RingBuffer(StubEventFactory(), new SingleThreadedClaimStrategy(16), new YieldingWaitStrategy)


  "New Ring Buffer " should {
    "fill all buckets with event factor" in {
      val rb  = buffer

      NumericRange(0L ,16L, 1L )
        .filter{ item => rb.get(item).data != item}
          .should(beEmpty)
    }

    "Circle when buffer size is outweighted " in {
      val rb  = buffer
      NumericRange(16L ,32L, 1L )
        .filter{ item => rb.get(item).data != item - 16}
          .should(beEmpty)
    }

    "throw some exception when number of buckets is not a power of 2" in {
      new RingBuffer(
        StubEventFactory(),
        new SingleThreadedClaimStrategy(17),
        new YieldingWaitStrategy).should(throwA)
    }
  }
}
