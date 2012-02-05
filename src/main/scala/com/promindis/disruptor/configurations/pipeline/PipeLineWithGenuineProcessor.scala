package com.promindis.disruptor.configurations.pipeline

import com.promindis.disruptor.adapters.RingBufferFactory._
import java.util.concurrent.CountDownLatch

import com.promindis.disruptor.adapters.EventModule.{Handler, ValueEvent, ValueEventFactory}
import com.promindis.disruptor.configurations.{Configuration, Scenario}
import com.promindis.disruptor.adapters.{Builder, Shooter, EventModule}
import Builder._
import com.promindis.disruptor.adapters.ProcessorLifeCycle._

object PipeLineWithGenuineProcessor extends Scenario{

  def challenge(implicit config: Configuration) = {
    val rb = ringBuffer(ValueEventFactory,size = config.ringBufferSize);
    val countDownLatch = new CountDownLatch(1)

    val chain = for {
      _ <- pipe[ValueEvent](Handler("C1"), rb)
      _ <- pipe[ValueEvent](Handler("C2"), rb)
      _ <- pipe[ValueEvent](Handler("C3", latch = Some(countDownLatch), expectedShoot = config.iterations), rb)
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