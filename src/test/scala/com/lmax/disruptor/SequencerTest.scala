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
    sequencer.setGatingSequences(sequence())
    sequencer
  }

  def sequence(value: Long = -1) = {
    val sequence = new Sequence()
    sequence.set(value)
    sequence
  }

  implicit def onSteroids(sequencer: Sequencer) = new {
    def withGating(sequences: Sequence*) = {
      sequencer.setGatingSequences(sequences: _*)
      sequencer
    }
  }

  implicit def onSteroids(descriptor: BatchDescriptor) = new {
    def withEnd(value: Long) = {
      descriptor.setEnd(value)
      descriptor
    }
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
      val sequence = new Sequence(5)
      val s = sequencer().withGating(sequence)

      NumericRange(0L,22L, 1L).map{
        i => s.next().should(beEqualTo(i))
      }

      val future = submitFragment{s.next()}
      sequence.set(6)
      future.get(2L, SECONDS).should(beEqualTo(22L))
    }

  }

  "incrementAndGet with timeout" should {

    "return result when cursor is lower when gating sequence" in {
      val s = sequencer().withGating(sequence(5L))
      s.next(2L, SECONDS).should(beEqualTo(0L))
      s.next(2L, SECONDS).should(beEqualTo(1L))
    }

    "return result after gating sequence has progressed normally" in {
      val sequence = new Sequence(5)
      val s = sequencer().withGating(sequence)

      NumericRange(0L,22L, 1L).map{
        i => s.next(2L, SECONDS).should(beEqualTo(i))
      }

      val future = submitFragment{s.next(2L, SECONDS)}
      sequence.set(6)
      future.get(2L, SECONDS).should(beEqualTo(22L))
    }

    "throw exception beyond timeout limit" in {
      val sequence = new Sequence(5)
      val s = sequencer().withGating(sequence)

      NumericRange(0L,22L, 1L).map{
        i => s.next(2L, SECONDS).should(beEqualTo(i))
      }

      val future = submitFragment{s.next(1L, SECONDS)}
      future.get(2L, SECONDS).should(throwA)

    }

  }

  "next with batch descriptor " should {
    "update batch descriptor content when there is available data" in {
        sequencer().withGating(sequence(5L)).next(new BatchDescriptor(4)).getEnd.should(beEqualTo(3))
    }

    "update batch descriptor content after data has been filled up" in {
      val sequence = new Sequence(5L)
      val s = sequencer().withGating(sequence)
      s.next(new BatchDescriptor(16)).getEnd.should(beEqualTo(15))

      val future = submitFragment{
        s.next(new BatchDescriptor(16))
      }

      sequence.set(64)
      future.get(2L, SECONDS).getEnd.should(beEqualTo(31L))

    }
  }

  "next with batch descriptor and timout" should {
    "update batch descriptor content when there is available data" in {
      sequencer().withGating(sequence(5L)).next(new BatchDescriptor(4), 1L , SECONDS).getEnd.should(beEqualTo(3))
    }

    "update batch descriptor content after data has been filled up" in {
      val sequence = new Sequence(5L)
      val s = sequencer().withGating(sequence)
      s.next(new BatchDescriptor(16), 1L , SECONDS).getEnd.should(beEqualTo(15))

      val future = submitFragment{
        s.next(new BatchDescriptor(16), 2L , SECONDS)
      }

      sequence.set(64)
      future.get(2L, SECONDS).getEnd.should(beEqualTo(31L))
    }

    "update batch descriptor content after data has been filled up" in {
      val sequence = new Sequence(5L)
      val s = sequencer().withGating(sequence)
      s.next(new BatchDescriptor(16), 1L , SECONDS).getEnd.should(beEqualTo(15))

      val future = submitFragment{
        s.next(new BatchDescriptor(16), 2L , SECONDS)
      }

      sequence.set(64)
      future.get(2L, SECONDS).getEnd.should(beEqualTo(31L))
    }

    "should throw exception after timeout without gating sequences update" in {
      val sequence = new Sequence(5L)
      val s = sequencer().withGating(sequence)
      s.next(new BatchDescriptor(16), 1L , SECONDS).getEnd.should(beEqualTo(15))

      val future = submitFragment{
        s.next(new BatchDescriptor(16), 2L , SECONDS)
      }

      future.get(2L, SECONDS).should(throwA)
    }

  }

  "force publish " should  {
    "update cursor " in {
      val s = sequencer()
      s.getCursor.should(beEqualTo(-1L))
      s.forcePublish(5L)
      s.getCursor.should(beEqualTo(5L))

    }
  }

  "publish" should  {
    "update cursor value" in {
      val s = sequencer()
      s.getCursor.should(beEqualTo(-1L))
      s.publish(5L)
      s.getCursor.should(beEqualTo(5L))
    }

    "update cursor value with batch end value" in {
      val s = sequencer()
      s.getCursor.should(beEqualTo(-1L))
      s.publish(new BatchDescriptor(16).withEnd(32L))
      s.getCursor.should(beEqualTo(32L))
    }

  }

}
