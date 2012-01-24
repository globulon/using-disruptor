package com.promindis.disruptor.configurations.diamond

import com.promindis.disruptor.adaptaters.EventModule._
import java.util.concurrent.CountDownLatch
import com.lmax.disruptor._
import com.promindis.disruptor.adaptaters.ProcessorLifeCycle._
import com.promindis.disruptor.adaptaters.RingBufferFactory._
import com.promindis.disruptor.adaptaters.TimeMeasurement._
import com.promindis.disruptor.adaptaters.Shooter

/**
 * Reproduces LMAX diamond configuration
 */
object Diamond {
  val RING_BUFFER_SIZE = 1024 * 1024
  val ITERATIONS = 1000L * 1000L * 10L
  val RUNS = 5


  def challenge(): Long = {

    val rb = ringBuffer(ValueEventFactory, RING_BUFFER_SIZE, new YieldingWaitStrategy());

    val firstBarrier = rb.newBarrier();
    val firstHandler = Handler("one")
    val secondHandler = Handler("two")
    val consumerOne = new BatchEventProcessor[ValueEvent](rb, firstBarrier, firstHandler)
    val consumerTwo = new BatchEventProcessor[ValueEvent](rb, firstBarrier, secondHandler)

    val secondBarrier = rb.newBarrier(consumerOne.getSequence, consumerTwo.getSequence);
    val countDownLatch = new CountDownLatch(1);
    val thirdHandler = Handler("three", latch = Some(countDownLatch), expectedShoot = ITERATIONS)
    val consumerThree = new BatchEventProcessor[ValueEvent](rb, secondBarrier, thirdHandler)

    rb.setGatingSequences(consumerThree.getSequence);

    val shooter = Shooter(ITERATIONS, rb, fillEvent)

    sampling {
      executing(consumerOne, consumerTwo, consumerThree) {
        shooter ! 'fire
        countDownLatch.await();
      }
    } provided {
      ITERATIONS.throughput(_)
    }

  }

  def main(args: Array[String]) {
    for (_ <- 1 to RUNS) {
      println("Nb Op/s: " + challenge())
    }
  }
}