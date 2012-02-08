package com.promindis.disruptor.port

import org.specs2.mutable.Specification
import com.lmax.disruptor.SingleThreadedClaimStrategy

/**
 * Date: 08/02/12
 * Time: 11:25
 */

final class ThreadedClaimStrategyTest() extends Specification{

  val BUFFER_SIZE = 128
  def claim = new SingleThreadedClaimStrategy(BUFFER_SIZE)

  "New ThreadedClaimStrategy " should {
    "map buffer size" in {
      claim.getBufferSize shouldEqual BUFFER_SIZE
    }

    "have 0 as initial sequence" in{
      claim.getSequence.shouldEqual(0)
    }
  }


}
