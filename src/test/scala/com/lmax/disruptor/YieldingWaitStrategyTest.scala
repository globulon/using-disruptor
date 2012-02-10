package com.lmax.disruptor

import org.specs2.mutable.Specification
import com.promindis.disruptor.EventModuleStub.ValueEventFactory
import com.lmax.disruptor.{Sequence => RSequence}
import com.promindis.disruptor.Tools._
import java.util.concurrent.TimeUnit
import TimeUnit._

/**
 * Date: 09/02/12
 * Time: 13:06
 */

final class YieldingWaitStrategyTest extends Specification {

  def rb = new RingBuffer(ValueEventFactory, new SingleThreadedClaimStrategy(128), new YieldingWaitStrategy())

  def gate = new RSequence(5)

  val sequenceOne = new RSequence(3)
  val sequenceTwo = new RSequence(7)

  "waitFor " should {

    "wait until cursor value reach expected sequence" in {
      val cursor = new RSequence()
      val result = submitFragment {
        new YieldingWaitStrategy().waitFor(5, cursor, Array(), rb.newBarrier())
      }
      cursor.set(8)
      result.get(2, SECONDS).should(beEqualTo(8L))
    }

    "wait until all dependant sequences values have progressed " in {
      val result = submitFragment {
        new YieldingWaitStrategy().waitFor(5, new RSequence(), Array(sequenceOne, sequenceTwo), rb.newBarrier())
      }

      sequenceOne.set(9L)
      sequenceTwo.set(10L)
      result.get(2, SECONDS).should(beEqualTo(9L))

    }
  }

  "waitFor with timeout" should {

    "return cursor value when found before ellapsed time" in {
      val cursor = new RSequence()
      val result = submitFragment {
        new YieldingWaitStrategy().waitFor(5, cursor, Array(), rb.newBarrier(), 10L, SECONDS)
      }
      cursor.set(8)
      result.get(2, SECONDS).should(beEqualTo(8L))
    }

    "return minmum of dependent sequences values when found" in {
      val result = submitFragment {
        new YieldingWaitStrategy().waitFor(5, new RSequence(), Array(sequenceOne, sequenceTwo), rb.newBarrier(), 10L, SECONDS)
      }

      sequenceOne.set(9L)
      sequenceTwo.set(10L)
      result.get(2, SECONDS).should(beEqualTo(9L))
    }

    "return cursor value after timeout elapsed" in {
      val cursor = new RSequence(4L)
      val result = submitFragment {
        new YieldingWaitStrategy().waitFor(5, cursor, Array(), rb.newBarrier(), 1L, SECONDS)
      }
      result.get(2, SECONDS).should(beEqualTo(4L))
    }

    "return minimum value in sequences after timeout elapsed" in {
      val result = submitFragment {
        new YieldingWaitStrategy().waitFor(5, new RSequence(), Array(sequenceOne, sequenceTwo), rb.newBarrier(), 1L, SECONDS)
      }
      result.get(2, SECONDS).should(beEqualTo(3L))
    }
  }
}
