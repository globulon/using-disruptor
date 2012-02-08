package com.promindis.disruptor.port

import org.specs2.mutable.Specification

/**
 * Date: 08/02/12
 * Time: 11:25
 */

final class SingleThreadedClaimStrategyTest extends Specification {

  val BUFFER_SIZE = 128

  def claim = SingleThreadedClaimStrategy(BUFFER_SIZE)

  "New ThreadedClaimStrategy " should {
    "map buffer size" in {
      claim.bufferSize shouldEqual BUFFER_SIZE
    }

    "have 0 as initial sequence" in {
      claim.sequence.shouldEqual(-1L)
    }
  }

  "hasAvailableCapacity " should {
    "return true when a dependent sequences allows it " in {
      claim.hasAvailableCapacity(1, RSequence(5L)).isSuccess
    }

    "return true when all dependent sequences allows it " in {
      claim.hasAvailableCapacity(1, RSequence(3L), RSequence(5L)).isSuccess
    }

    "return false when minmum dependent sequences does not allow it" in {
      val aClaim = claim
      aClaim.setSequence(131L, RSequence(3L), RSequence(5L))
      aClaim.hasAvailableCapacity(1, RSequence(3L), RSequence(5L)).isFailure
    }
  }

  "setSequence " should {
    "update sequence with greater dependent sequences values " in {
      val aClaim = claim
      aClaim.setSequence(1, RSequence(3L), RSequence(5L))
      aClaim.hasAvailableCapacity(1, RSequence(3L), RSequence(5L)).isSuccess
    }
  }

  "serialisePublishing" should {
    "collect sequence value as cursor value" in {
      val sequence = RSequence()
      claim.serialisePublishing(17L, sequence, 8)
      sequence.get().should(beEqualTo(17L))
    }
  }

  "increment and Get " should {
    "increment value while possible " in {
      val aClaim = claim
      Range(1,133).foreach( _ => aClaim.incrementAndGet(RSequence(3L), RSequence(5L)))
      aClaim.hasAvailableCapacity(1, RSequence(3L), RSequence(5L)).isFailure
    }

    "increment with delta if  possible " in {
      val aClaim = claim
      aClaim.incrementAndGet(132, RSequence(3L), RSequence(5L))
      aClaim.hasAvailableCapacity(1, RSequence(3L), RSequence(5L)).isFailure
    }
  }

}
