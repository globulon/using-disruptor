package com.promindis.disruptor.configurations.unicast

import com.promindis.disruptor.adapters.RingBufferFactory._
import com.promindis.disruptor.adapters.EventModule.{Handler, ValueEvent, ValueEventFactory}
import java.util.concurrent.CountDownLatch
import com.promindis.disruptor.configurations.{Configuration, Scenario}
import com.promindis.disruptor.adapters.Builder._
import com.promindis.disruptor.adapters.{ProcessorFactory, EventModule, Shooter}

object Unicast extends Scenario {

  def challenge(implicit config: Configuration, factory: ProcessorFactory) = {
    val rb = ringBuffer(ValueEventFactory, size = config.ringBufferSize);
    val countDownLatch = new CountDownLatch(1)

    val chain = for {
      _ <- pipe(Handler("Consumer", config.iterations, Some(countDownLatch)), rb)
    } yield ()

    val consumers = chain(List())._2
    val processors = consumers.unzip._1
    rb.setGatingSequences(processors.head.getSequence)
    val shooter = Shooter(config.iterations, rb, EventModule.fillEvent)

    playWith(processors) {
      shooter ! 'fire
      countDownLatch.await()
    }
  }


}