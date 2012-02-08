package com.promindis.disruptor.genuine

import org.specs2.mutable.Specification
import com.lmax.disruptor.{SingleThreadedClaimStrategy, YieldingWaitStrategy, Sequencer, Sequence}

/**
 * Date: 08/02/12
 * Time: 10:06
 */
final class SequencerTest extends Specification {

  def sequencer() = {
    val sequencer = new Sequencer(new SingleThreadedClaimStrategy(128), new YieldingWaitStrategy())
    sequencer.setGatingSequences(sequence)
    sequencer
  }

  def sequence = {
    val sequence = new Sequence()
    sequence.set(-1)
    sequence
  }

  "Initialized Sequencer" should {
    "have same buffer size as claim strategy" in {
      sequencer().getBufferSize.shouldEqual(128)
    }

    "increment sequence number " in {
      val s = sequencer()
      s next() shouldEqual (0) and (s next() shouldEqual (1))
    }
  }

  "new Batch descriptor " should {
    "produce batch sequence matching input size lower than capacity" in {
      sequencer().newBatchDescriptor(32).getSize.shouldEqual(32)
    }

    "produce batch sequence limited to claim strategy size with over sized input" in {
      sequencer().newBatchDescriptor(256).getSize.shouldEqual(128)
    }
  }

  "New barrier " should {
    "produce new instance " in {
      val s = sequencer()
      s.newBarrier(new Sequence()).getCursor.shouldEqual(s.getCursor)

    }
  }


}
