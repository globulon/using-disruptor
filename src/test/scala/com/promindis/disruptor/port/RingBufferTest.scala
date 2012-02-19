package com.promindis.disruptor.port

import org.specs2.mutable.Specification
import collection.immutable.NumericRange

/**
 * Date: 18/02/12
 * Time: 18:33
 */

final class RingBufferTest extends Specification {
  def buffer = RingBuffer(StubEventFactory(), SingleThreadedClaimStrategy(16), YieldingWaitStrategy())

  "New Ring Buffer " should {
    "fill all buckets with event factor" in {
      val rb = buffer.get

      NumericRange(0L, 16L, 1L).filter {
        item => rb.get(item).data != item
      }.should(beEmpty)
    }

    "Circle when buffer size is outweighted " in {
      val rb = buffer.get
      NumericRange(16L, 32L, 1L)
        .filter {
        item => rb.get(item).data != item - 16
      }
        .should(beEmpty)
    }

    "not return any ring if buffer size is not a power of 2" in {
      RingBuffer(
        StubEventFactory(),
        SingleThreadedClaimStrategy(17),
        YieldingWaitStrategy()).should(beNone)
    }
  }
}
