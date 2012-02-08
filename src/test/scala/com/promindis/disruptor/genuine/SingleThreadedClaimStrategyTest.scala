package com.promindis.disruptor.genuine

import org.specs2.mutable.Specification
import com.lmax.disruptor.{SingleThreadedClaimStrategy, Sequence}

/**
 * Date: 08/02/12
 * Time: 11:25
 */

final class SingleThreadedClaimStrategyTest() extends Specification {

  val BUFFER_SIZE = 128

  def claim = new SingleThreadedClaimStrategy(BUFFER_SIZE)

  "New ThreadedClaimStrategy " should {
    "map buffer size" in {
      claim.getBufferSize shouldEqual BUFFER_SIZE
    }

    "have 0 as initial sequence" in {
      claim.getSequence.shouldEqual(-1L)
    }
  }

  "hasAvailableCapacity " should {
    "return true when a dependent sequences allows it " in {
      claim.hasAvailableCapacity(1, Array(new Sequence(5L))).isSuccess
    }

    "return true when all dependent sequences allows it " in {
      val sequences = Array(new Sequence(3L), new Sequence(5L))
      claim.hasAvailableCapacity(1, sequences).isSuccess
    }

    "return false when minmum dependent sequences does not allow it" in {
      val sequences = Array(new Sequence(3L), new Sequence(5L))
      val aClaim = claim
      aClaim.setSequence(131L, sequences)
      aClaim.hasAvailableCapacity(1, sequences).isFailure
    }
  }

  "setSequence " should {
    "update sequence with greater dependent sequences values " in {
      val sequences = Array(new Sequence(3L), new Sequence(5L))
      val aClaim = claim
      aClaim.setSequence(1, sequences)
      aClaim.hasAvailableCapacity(1, sequences).isSuccess
    }
  }

  "serialisePublishing" should {
    "collect sequence value as cursor value" in {
      val sequence = new Sequence()
      claim.serialisePublishing(17L, sequence, 8)
      sequence.get().should(beEqualTo(17L))
    }
  }

  "increment and Get " should {
    "increment value while possible " in {
      val sequences = Array(new Sequence(3L), new Sequence(5L))
      val aClaim = claim
      Range(1,133).foreach( _ => aClaim.incrementAndGet(sequences))
      aClaim.hasAvailableCapacity(1, sequences).isFailure
    }

    "increment with delta if  possible " in {
      val sequences = Array(new Sequence(3L), new Sequence(5L))
      val aClaim = claim
      aClaim.incrementAndGet(132, sequences)
      aClaim.hasAvailableCapacity(1, sequences).isFailure
    }
  }


}
