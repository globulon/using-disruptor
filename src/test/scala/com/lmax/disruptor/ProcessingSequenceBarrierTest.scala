package com.lmax.disruptor

import org.specs2.mutable.Specification
import com.lmax.disruptor.{Sequence => RSequence}
import java.util.concurrent.TimeUnit._


/**
 * Date: 09/02/12
 * Time: 10:29
 */

final class ProcessingSequenceBarrierTest extends Specification {

  "New ProcessingSequenceBarrier " should {
    "not be alerted" in {
      new ProcessingSequenceBarrier(FakeWaitStrategy(), new RSequence(), Array()).isAlerted.should(beFalse)
    }

    "have same cursor value as ring buffer" in {
      val cursor = new RSequence(3L)
      val barrier = new ProcessingSequenceBarrier(FakeWaitStrategy(), cursor, Array())
      barrier.getCursor.should(beEqualTo(3L))
      cursor.set(5L)
      barrier.getCursor.should(beEqualTo(5L))
    }
  }

  "Alert" should {
    "Update alert flag " in {
      val barrier = new ProcessingSequenceBarrier(FakeWaitStrategy(), new RSequence(), Array())
      barrier.isAlerted.should(beFalse)
      barrier.alert()
      barrier.isAlerted.should(beTrue)
    }

    "be cleared with clearAlert command" in {
      val barrier = new ProcessingSequenceBarrier(FakeWaitStrategy(), new RSequence(), Array())
      barrier.isAlerted.should(beFalse)
      barrier.alert()
      barrier.isAlerted.should(beTrue)
      barrier.clearAlert()
      barrier.isAlerted.should(beFalse)
    }
  }

  "check alert " should {
    "throw exception on alert" in {
      val barrier = new ProcessingSequenceBarrier(FakeWaitStrategy(), new RSequence(), Array())
      barrier.alert()
      barrier.isAlerted.should(beTrue)
      barrier.checkAlert().should(throwA)
    }
  }

  "waitFor sequence" should {
    "use wait strategy to wait" in {
      val waitStrategy = FakeWaitStrategy()
      val barrier = new ProcessingSequenceBarrier(waitStrategy, new RSequence(), Array())
      barrier.waitFor(5L)
      waitStrategy.waitedSequence.should(beEqualTo(5L))
    }

    "if alerted should throw exception" in {
      val waitStrategy = FakeWaitStrategy()
      val barrier = new ProcessingSequenceBarrier(waitStrategy, new RSequence(), Array())
      barrier.alert()
      barrier.waitFor(5L).should(throwA)
    }

    "use wait strategy to wait with timeout" in {
      val waitStrategy = FakeWaitStrategy()
      val barrier = new ProcessingSequenceBarrier(waitStrategy, new RSequence(), Array())
      barrier.waitFor(5L, 2, SECONDS)
      waitStrategy.waitedSequence.should(beEqualTo(5L))
    }

    "if alerted should throw exception even with timeout" in {
      val waitStrategy = FakeWaitStrategy()
      val barrier = new ProcessingSequenceBarrier(waitStrategy, new RSequence(), Array())
      barrier.alert()
      barrier.waitFor(5L).should(throwA)
    }
  }
}
