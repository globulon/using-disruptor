package com.lmax.disruptor

import org.specs2.mutable.Specification
import collection.immutable.NumericRange
import com.promindis.disruptor.Tools._
import java.util.concurrent.TimeUnit._

/**
 * Date: 08/02/12
 * Time: 10:06
 */
final class SequencerTest extends Specification {

  def sequencer() = {
    val sequencer = new Sequencer(new SingleThreadedClaimStrategy(16), new YieldingWaitStrategy())
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
      sequencer().getBufferSize.shouldEqual(16)
    }

    "increment sequence number " in {
      val s = sequencer()
      s next() shouldEqual (0) and (s next() shouldEqual (1))
    }
  }

  "new Batch descriptor " should {
    "produce batch sequence matching input size lower than capacity" in {
      sequencer().newBatchDescriptor(8).getSize.shouldEqual(8)
    }

    "produce batch sequence limited to claim strategy size with over sized input" in {
      sequencer().newBatchDescriptor(128).getSize.shouldEqual(16)
    }
  }

  "New barrier " should {
    "produce barrier holding on cursor" in {
      val s = sequencer()
      s.newBarrier(new Sequence()).getCursor.shouldEqual(s.getCursor)
    }
  }

  "incrementAndGet " should {

    //todo exception throwing

    "return result when cursor is lower when gating sequence" in {
      val s = sequencer()
      s.setGatingSequences(new Sequence(5))
      s.next().should(beEqualTo(0L))
      s.next().should(beEqualTo(1L))
    }

    "return result after gating sequence has progressed" in {
      val s = sequencer()
      val gatingSequence = new Sequence(5)
      s.setGatingSequences()
      NumericRange(0L,22L, 1L).map{
        i => s.next().should(beEqualTo(i))
      }
      val future = submitFragment{s.next()}
      gatingSequence.set(6)
      future.get(2L, SECONDS).should(beEqualTo(22L))
    }

  }
}
