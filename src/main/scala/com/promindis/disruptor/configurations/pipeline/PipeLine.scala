package com.promindis.disruptor.configurations.pipeline

import com.promindis.disruptor.adapters.RingBufferFactory._
import java.util.concurrent.CountDownLatch

import com.promindis.disruptor.adapters.EventModule.{Handler, ValueEvent, ValueEventFactory}
import com.promindis.disruptor.configurations.{Configuration, Scenario}
import com.promindis.disruptor.adapters.{ProcessorFactory, Builder, Shooter, EventModule}
import Builder._
import com.promindis.disruptor.port.EventHandler

object PipeLine extends Scenario{

  def challenge(implicit config: Configuration, factory: ProcessorFactory) = {
    val rb = ringBuffer(ValueEventFactory,size = config.ringBufferSize);
    val countDownLatch = new CountDownLatch(1)

    val chain = for {
      _ <- pipe(Handler("C1"), rb)
      _ <- pipe(Handler("C2"), rb)
      _ <- pipe(Handler("C3", latch = Some(countDownLatch), expectedShoot = config.iterations), rb)
    } yield ()

    val consumers = chain(List())._2
    val processors = consumers.unzip._1
    rb.setGatingSequences(processors.head.getSequence)
    val shooter = Shooter(config.iterations, rb, EventModule.fillEvent)

    playWith(processors){
      shooter ! 'fire
      countDownLatch.await()
    }

  }
}