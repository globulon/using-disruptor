package com.promindis.disruptor.configurations.diamond

import com.promindis.disruptor.adapters.EventModule._
import java.util.concurrent.CountDownLatch
import com.lmax.disruptor._
import com.promindis.disruptor.adapters.RingBufferFactory._
import com.promindis.disruptor.adapters.Shooter
import com.promindis.disruptor.configurations.{Configuration, Scenario}

/**
 * Reproduces LMAX diamond configuration
 */
object Diamond extends Scenario{

  def challenge(implicit config: Configuration): Long = {

    val rb = ringBuffer(ValueEventFactory, config.ringBufferSize, new YieldingWaitStrategy());

    val firstBarrier = rb.newBarrier();
    val firstHandler = Handler("one")
    val secondHandler = Handler("two")
    val consumerOne = new BatchEventProcessor[ValueEvent](rb, firstBarrier, firstHandler)
    val consumerTwo = new BatchEventProcessor[ValueEvent](rb, firstBarrier, secondHandler)

    val secondBarrier = rb.newBarrier(consumerOne.getSequence, consumerTwo.getSequence);
    val countDownLatch = new CountDownLatch(1);
    val thirdHandler = Handler("three", latch = Some(countDownLatch), expectedShoot = config.iterations)
    val consumerThree = new BatchEventProcessor[ValueEvent](rb, secondBarrier, thirdHandler)

    rb.setGatingSequences(consumerThree.getSequence);

    val shooter = Shooter(config.iterations, rb, fillEvent)

    playWith (List(consumerOne, consumerTwo, consumerThree)){
      shooter ! 'fire
      countDownLatch.await();
    }

  }

  def main(args: Array[String]) { run(Configuration())}
}