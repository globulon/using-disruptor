package com.promindis.disruptor.configurations.diamond

import com.promindis.disruptor.adapters.EventModule._
import java.util.concurrent.CountDownLatch

import com.promindis.disruptor.adapters.RingBufferFactory._
import com.promindis.disruptor.configurations.{Configuration, Scenario}
import com.promindis.disruptor.adapters.Builder._
import com.promindis.disruptor.adapters.{ProcessorFactory, EventModule, Shooter}
import com.promindis.disruptor.port.YieldingWaitStrategy

/**
* Reproduces LMAX diamond configuration
*/
object Diamond extends Scenario {

  def challenge(implicit config: Configuration, factory: ProcessorFactory): Long = {

    val Some(rb) = ringBuffer(ValueEventFactory, config.ringBufferSize, YieldingWaitStrategy());

    val countDownLatch = new CountDownLatch(1);

    val diamond = for {
      barrier <- fork(Handler("C1"), rb, rb.barrier)
      _ <- fork(Handler("C2"), rb, barrier)
      _ <- join(Handler("C3", latch = Some(countDownLatch), expectedShoot = config.iterations), rb)
    } yield ()

    val consumers = diamond(List())._2
    val processors = consumers.unzip._1
    rb.withGating(processors.head.getSequence)
    val shooter = Shooter(config.iterations, rb, EventModule.fillEvent)

    playWith(processors) {
      shooter ! 'fire
      countDownLatch.await()
    }
  }

}