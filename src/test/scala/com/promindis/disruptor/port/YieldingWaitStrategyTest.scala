package com.promindis.disruptor.port

import org.specs2.mutable.Specification
import com.promindis.disruptor.Tools._
import java.util.concurrent.TimeUnit
import TimeUnit._
import scala.actors.threadpool.{TimeUnit => Unit}


/**
 * Date: 09/02/12
 * Time: 13:06
 */

final class YieldingWaitStrategyTest extends Specification {

  def setup = {
    val cursor = RSequence()
    val waitStrategy = YieldingWaitStrategy()
    val barrier = SequencesBarrier(waitStrategy, cursor)
    (waitStrategy, cursor, barrier)
  }

  def dependentSequences() = Seq(RSequence(3L), RSequence(7L))

  "waitFor " should {

    "wait until cursor value reach expected sequence" in {
      val (waitStrategy, cursor, barrier) = setup
      val result = submitFragment {
        waitStrategy.waitFor(5L, cursor, barrier)
      }
      cursor.set(8)
      result.get(2, SECONDS).should(beEqualTo(Some(8L)))
    }

    "wait until all dependant sequences values have progressed " in {
      val (waitStrategy, cursor, barrier) = setup
      val sequences = dependentSequences()

      val result = submitFragment {
        waitStrategy.waitFor(5, barrier, sequences)
      }

      sequences(0).set(9L)
      result.get(2, SECONDS).should(beEqualTo(Some(7L)))
    }
  }

  "waitFor with timeout" should {

    "return cursor value when found before ellapsed time" in {
      val (waitStrategy, cursor, atBarrier) = setup
      val result = submitFragment {
        waitStrategy.waitFor(5L,  Unit.SECONDS, 5L, cursor, atBarrier)
      }
      cursor.set(8)
      result.get(2, SECONDS).should(beEqualTo(Some(8L)))
    }
    "return minmum of dependent sequences values when found" in {
      val (waitStrategy, cursor, atBarrier) = setup
      val sequences = dependentSequences()

      val result = submitFragment {
        waitStrategy.waitFor(5L,  Unit.SECONDS, 5L, atBarrier, sequences)
      }

      sequences(0).set(9L)
      result.get(2, SECONDS).should(beEqualTo(Some(7L)))
    }

    "return cursor value after timeout elapsed" in {
      val (waitStrategy, _, atBarrier) = setup

      val result = submitFragment {
        waitStrategy.waitFor(1L, Unit.SECONDS, 5L, RSequence(4L), atBarrier)
      }

      result.get(2, SECONDS).should(beEqualTo(Some(4L)))
    }

    "return minimum value in sequences after timeout elapsed" in {
      val (waitStrategy, cursor, atBarrier) = setup

      val result = submitFragment {
        waitStrategy.waitFor(1L, Unit.SECONDS, 5L, atBarrier, dependentSequences())
      }
      result.get(2, SECONDS).should(beEqualTo(Some(3L)))
    }
  }
}
