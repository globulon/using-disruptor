package com.promindis.disruptor.port
import org.specs2.mutable.Specification
import Sequences._
/**
 * Date: 08/02/12
 * Time: 11:34
 */

final class SequenceTest extends Specification {

  "Sequence invoked with default constructor" should {
    "have the sequencer initial value" in {
      Sequence().get().should(beEqualTo(INITIAL_VALUE))
    }
  }

  "Sequence is a container that " should {
    "embed a provided sequence number" in {
      Sequence(128L).get().shouldEqual(128L)
    }

    "update embedded sequence on need" in {
      Sequence(128L).updated(64L).get().shouldEqual(64L)
    }

    "update embedded sequence on need with expected sequence" in {
      Sequence(128L).updated(64L).compareAndSet(64L, 65L).isSuccess
    }

    "not update embedded sequence when the  missing the  expected sequence" in {
      Sequence(128L).updated(64L).compareAndSet(62L, 63L).isFailure
    }

  }

}
