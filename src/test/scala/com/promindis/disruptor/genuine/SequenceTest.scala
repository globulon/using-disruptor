package com.promindis.disruptor.genuine

import org.specs2.mutable.Specification
import com.lmax.disruptor.{Sequence => PaddedSequence}

/**
 * Date: 08/02/12
 * Time: 11:34
 */

final class SequenceTest extends Specification {

  def updated(seq: PaddedSequence, withValue: Long) = {
    seq.set(withValue)
    seq
  }

  def paddedSequence = new PaddedSequence(128)

  "Sequence invoked with default constructor" should {
    "have the sequencer initial value" in {
      new PaddedSequence().get.shouldEqual(com.lmax.disruptor.Sequencer.INITIAL_CURSOR_VALUE)
    }
  }


  "Sequence is a container that " should {
    "embed a provided sequence number" in {
      paddedSequence.get().shouldEqual(128L)
    }

    "update embedded sequence on need" in {
      updated(paddedSequence, 64L).get().shouldEqual(64L)
    }

    "update embedded sequence on need with expected sequence" in {
      updated(paddedSequence, 64L).compareAndSet(64L, 65L).isSuccess
    }

    "not update embedded sequence when the  missing the  expected sequence" in {
      updated(paddedSequence, 64L).compareAndSet(62L, 63L).isFailure
    }

    "update padding sequence on need" in {
      val seq = paddedSequence
      seq.setPaddingValue(32L)
      seq.sumPaddingToPreventOptimisation().shouldEqual(576L)
    }

  }

}
