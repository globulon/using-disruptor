package com.promindis.disruptor.port

import org.specs2.mutable.Specification
import collection.immutable.NumericRange
import com.promindis.disruptor.Tools._
import scala.actors.threadpool.TimeUnit._
import java.util.concurrent.{TimeUnit => JTimeUnit}


/**
 * Date: 08/02/12
 * Time: 10:06
 */
class SequencerTest extends Specification {


  def sequencer: Sequencer = {
    Sequencer(SingleThreadedClaimStrategy(16), YieldingWaitStrategy())
  }

  "Initialized Sequencer" should {
    "have same buffer size as claim strategy" in {
      sequencer.bufferSize.shouldEqual(16)
    }

    "should return None if no gating sequence have been set" in {
      sequencer.next().should(beNone)
    }

    "increment sequence number " in {
      val s = sequencer.withGating(RSequence())
      s next() shouldEqual (Some(0)) and (s next() shouldEqual (Some(1)))
    }
  }

  "new Batch descriptor " should {
    "produce batch sequence matching input size lower than capacity" in {
      sequencer.batchDescriptor(8).size.shouldEqual(8)
    }

    "produce batch sequence limited to claim strategy size with over sized input" in {
      sequencer.batchDescriptor(128).size.shouldEqual(16)
    }
  }

  "New barrier " should {
    "produce barrier holding on cursor" in {
      val s = sequencer
      s.barrierFor(RSequence()).cursorValue.shouldEqual(s.cursorValue)
    }
  }

  "incrementAndGet " should {

    "return result when cursor is lower when gating sequence" in {
      val s = sequencer.withGating(RSequence(5L))
      s.next().should(beEqualTo(Some(0L)))
        .and(s.next().should(beEqualTo(Some(1L))))
    }


    "return result after gating sequence has progressed" in {
      val sequence = RSequence(5L)
      val s = sequencer.withGating(sequence)

      NumericRange(0L, 22L, 1L).map {
        i => s next() should(beEqualTo(Some(i)))
      }

      val future = submitFragment {
        s next()
      }
      sequence set(6)
      future.get(2L, JTimeUnit.SECONDS) should(beEqualTo(Some(22L)))
    }
  }


  "incrementAndGet with timeout" should {

    "return result when cursor is lower when gating sequence" in {
      val s = sequencer.withGating(RSequence(5L))
      s.next(2L, SECONDS).should(beEqualTo(Some(0L)))
      s.next(2L, SECONDS).should(beEqualTo(Some(1L)))
    }

    "return result after gating sequence has progressed normally" in {
      val sequence = RSequence(5L)
      val s = sequencer.withGating()

      NumericRange(0L, 22L, 1L).map {
        i => s.next(2L, SECONDS).should(beEqualTo(Some(i)))
      }

      val future = submitFragment {
        s.next(2L, SECONDS)
      }

      sequence.set(6)
      future.get(2L, JTimeUnit.SECONDS).should(beEqualTo(Some(22L)))
    }

    "return no value beyond limit " in {
      val s = sequencer.withGating(RSequence(5L))

      NumericRange(0L, 22L, 1L).map {
        i => s.next(2L, SECONDS).should(beEqualTo(Some(i)))
      }

      val future = submitFragment {
        s.next(1L, SECONDS)
      }
      future.get(2L, JTimeUnit.SECONDS).should(beNone)
    }

  }

  "next with batch descriptor " should {
    "update batch descriptor content when there is available data" in {
      sequencer.withGating(RSequence(5L))
        .next(BatchDescriptor(4))
          .get.end.should(beEqualTo(3))
    }

    "update batch descriptor content after data has been filled up" in {
      val sequence = RSequence(5L)
      val s = sequencer.withGating(sequence)

      s.next(BatchDescriptor(16))
        .get.end.should(beEqualTo(15))

      val future = submitFragment{
        s.next(new BatchDescriptor(16))
      }

      sequence.set(64)
      future.get(2L, JTimeUnit.SECONDS)
        .get.end.should(beEqualTo(31L))
    }
  }

  "next with batch descriptor and timout" should {
    "update batch descriptor content when there is available data" in {
      sequencer.withGating(RSequence(5L))
        .next(BatchDescriptor(4), 1L, SECONDS).get.end.should(beEqualTo(3))
    }

    "update batch descriptor content after data has been filled up" in {
      val sequence = RSequence(5L)
      val s = sequencer.withGating(sequence)
      s.next(BatchDescriptor(16), 1L, SECONDS).get.end.should(beEqualTo(15))

      val future = submitFragment {
        s.next(new BatchDescriptor(16), 2L, SECONDS)
      }

      sequence.set(64)
      future.get(2L, JTimeUnit.SECONDS).get.end.should(beEqualTo(31L))
    }

    "update batch descriptor content after data has been filled up" in {
      val sequence = RSequence(5L)
      val s = sequencer.withGating(sequence)
      s.next(BatchDescriptor(16), 1L, SECONDS).get.end.should(beEqualTo(15))

      val future = submitFragment {
        s.next(BatchDescriptor(16), 2L, SECONDS)
      }

      sequence.set(64)
      future.get(2L, JTimeUnit.SECONDS).get.end.should(beEqualTo(31L))
    }


    "should throw exception after timeout without gating sequences update" in {
      val sequence = RSequence(5L)
      val s = sequencer.withGating(sequence)
      s.next(BatchDescriptor(16), 1L, SECONDS).get.end.should(beEqualTo(15))

      val future = submitFragment {
        s.next(BatchDescriptor(16), 1L, SECONDS)
      }

      future.get(2L, JTimeUnit.SECONDS).should(beNone)
    }
  }

  "force publish " should {
    "update cursor " in {
      val s = sequencer
      s.cursorValue.should(beEqualTo(-1L))
      s.forcePublish(5L)
      s.cursorValue.should(beEqualTo(5L))

    }
  }

  "publish" should {
    "update cursor value" in {
      val s = sequencer.withGating(RSequence(5L))
      s.cursorValue.should(beEqualTo(-1L))
      s.publish(5L)
      s.cursorValue.should(beEqualTo(5L))
    }

    "update cursor value with batch end value" in {
      val s = sequencer.withGating(RSequence(5L))
      s.cursorValue.should(beEqualTo(-1L))
      s.publish(BatchDescriptor(16L).withEnd(32L))
      s.cursorValue.should(beEqualTo(32L))

    }
  }
}
