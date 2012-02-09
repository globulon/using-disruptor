package com.promindis.disruptor.genuine

import org.specs2.mutable.Specification
import com.promindis.disruptor.EventModuleStub._
import com.lmax.disruptor.{SequenceBarrier, SingleThreadedClaimStrategy, RingBuffer, YieldingWaitStrategy, ProcessingSequenceBarrier, Sequence => RSequence}
import java.util.concurrent.CountDownLatch
import java.util.concurrent._
import TimeUnit._
import Executors._
import com.promindis.disruptor.Tools._

/**
 * Date: 09/02/12
 * Time: 10:29
 */

final class ProcessingSequenceBarrierTest extends Specification {
  def rb = new RingBuffer(ValueEventFactory, new SingleThreadedClaimStrategy(128), new YieldingWaitStrategy())

  def gate = new RSequence(5)

  val sequenceOne = new RSequence(3)
  val sequenceTwo = new RSequence(7)


  def barrierFrom[T](rb: RingBuffer[T]): SequenceBarrier = {
    rb.newBarrier(sequenceOne, sequenceTwo)
  }

  "New ProcessingSequenceBarrier " should {
    "not be alerted" in {
      barrierFrom(rb).isAlerted.should(beFalse)
    }

    "have same cursor value as ring buffer" in {
      val buffer = rb
      buffer.setGatingSequences(gate)
      buffer.publish(3L)
      buffer.getCursor.should(beEqualTo(3L))
        .and(barrierFrom(buffer).getCursor.should(beEqualTo(3L)))
    }
  }

  "Alert" should {
    "Update alert flag " in {
      val barrier = barrierFrom(rb)
      barrier.isAlerted.should(beFalse)
      barrier.alert()
      barrier.isAlerted.should(beTrue)
    }

    "be cleared with clearAlert command" in {
      val barrier = barrierFrom(rb)
      barrier.isAlerted.should(beFalse)
      barrier.alert()
      barrier.isAlerted.should(beTrue)
      barrier.clearAlert()
      barrier.isAlerted.should(beFalse)
    }
  }

  "check alert " should {
    "throw exception on alert" in {
      val barrier = barrierFrom(rb)
      barrier.alert()
      barrier.isAlerted.should(beTrue)
      barrier.checkAlert().should(throwA)
    }
  }



  "waitFor sequence" should {
    "wait until sequences are updated" in {
      def waitFor(barrier: SequenceBarrier) = new Callable[Boolean] {
        override def call() =           {
          barrier.waitFor(8)
          true
        }
      }

      val barrier = barrierFrom(rb)
      val future = submit(waitFor(barrier))
      List(sequenceOne, sequenceTwo).foreach(_.set(10))
      future.get(2, SECONDS).isSuccess
    }
  }

}
