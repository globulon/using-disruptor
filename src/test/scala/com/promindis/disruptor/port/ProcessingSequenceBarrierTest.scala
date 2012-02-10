package com.promindis.disruptor.port

import org.specs2.mutable.Specification
import actors.threadpool.TimeUnit._


/**
 * Date: 09/02/12
 * Time: 10:29
 */

final class ProcessingSequenceBarrierTest extends Specification {

  "New ProcessingSequenceBarrier " should {
    "not be alerted" in {
      ProcessingSequencesBarrier(FakeWaitStrategy(), RSequence()).alerted.should(beFalse)
    }
  }

  "Alert" should {
    "Update alert flag " in {
      val barrier = ProcessingSequencesBarrier(FakeWaitStrategy(), RSequence())
      barrier.alerted.should(beFalse)
      barrier.doAlert()
      barrier.alerted.should(beTrue)
    }

    "be cleared with clearAlert command" in {
      val barrier = new ProcessingSequencesBarrier(FakeWaitStrategy(), RSequence())
      barrier.alerted.should(beFalse)
      barrier.doAlert()
      barrier.alerted.should(beTrue)
      barrier.clearAlert()
      barrier.alerted.should(beFalse)
    }
  }

  "check alert " should {
    "throw exception on alert" in {
      val barrier = ProcessingSequencesBarrier(FakeWaitStrategy(), RSequence())
      barrier.doAlert()
      barrier.alerted.should(beTrue)
    }
  }

  "waitFor sequence" should {
    "use wait strategy to wait" in {
      val waitStrategy = FakeWaitStrategy()
      val cursor = RSequence()
      val barrier = new ProcessingSequencesBarrier(waitStrategy, cursor)
      barrier.waitFor(5L)
      waitStrategy.waitedSequence.should(beEqualTo(5L))
      waitStrategy.inputCursor.should(beEqualTo(cursor))
    }

    "if alerted should throw exception" in {
      val waitStrategy = FakeWaitStrategy()
      val barrier = new ProcessingSequencesBarrier(waitStrategy, RSequence())
      barrier.doAlert()
      barrier.waitFor(5L).should(beNone)
    }

    "use wait strategy to wait with timeout" in {
      val waitStrategy = FakeWaitStrategy()
      val cursor = RSequence()
      val barrier = ProcessingSequencesBarrier(waitStrategy, cursor)
      barrier.waitFor(2, SECONDS, 5L)
      waitStrategy.waitedSequence.should(beEqualTo(5L))
      waitStrategy.inputCursor.should(beEqualTo(cursor))
    }

    "if alerted should throw exception even with timeout" in {
      val waitStrategy = FakeWaitStrategy()
      val barrier = ProcessingSequencesBarrier(waitStrategy, RSequence())
      barrier.doAlert()
      barrier.waitFor(5L).should(beNone)
    }
  }
}
