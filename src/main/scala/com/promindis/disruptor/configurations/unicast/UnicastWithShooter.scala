package com.promindis.disruptor.configurations.unicast

import com.promindis.disruptor.adapters.RingBufferFactory._
import com.lmax.disruptor.BatchEventProcessor
import com.promindis.disruptor.adapters.EventModule.{Handler, ValueEvent, ValueEventFactory}
import java.util.concurrent.CountDownLatch
import com.promindis.disruptor.adapters.{EventModule, Shooter}
import com.promindis.disruptor.configurations.{Configuration, Scenario}


object UnicastWithShooter extends Scenario{

  override def challenge(implicit config: Configuration) = {
    val rb = ringBuffer(ValueEventFactory,size = config.ringBufferSize);

    val barrier =  rb.newBarrier()
    val countDownLatch = new CountDownLatch(1)
    val handler = Handler("P1", latch = Some(countDownLatch), expectedShoot = config.iterations)
    val processor = new BatchEventProcessor[ValueEvent](rb, barrier, handler);
    rb.setGatingSequences(processor.getSequence)

    val shooter = Shooter(config.iterations, rb, EventModule.fillEvent)

    playWith(List(processor)){
      shooter ! 'fire
      countDownLatch.await()
    }
  }
}