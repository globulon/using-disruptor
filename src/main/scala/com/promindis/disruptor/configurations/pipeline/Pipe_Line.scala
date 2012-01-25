package com.promindis.disruptor.configurations.pipeline

import com.promindis.disruptor.adapters.RingBufferFactory._
import java.util.concurrent.CountDownLatch

import com.promindis.disruptor.adapters.EventModule.{Handler, ValueEvent, ValueEventFactory}
import com.promindis.disruptor.adapters.{Shooter, EventModule}
import com.promindis.disruptor.configurations.{Configuration, Scenario}
import com.lmax.disruptor.BatchEventProcessor


object Pipe_Line extends Scenario{



  def challenge(implicit config: Configuration) = {
    val rb = ringBuffer(ValueEventFactory,size = config.ringBufferSize);

    val countDownLatch = new CountDownLatch(1)
    val handlerOne = Handler("P1")
    val handlerTwo = Handler("P2")
    val handlerThree = Handler("P3", latch = Some(countDownLatch), expectedShoot = config.iterations)

    val barrier =  rb.newBarrier()
    val consumerOne = new BatchEventProcessor[ValueEvent](rb, barrier, handlerOne);

    val consumerTwo = new BatchEventProcessor[ValueEvent](rb, rb.newBarrier(consumerOne.getSequence), handlerTwo);

    val consumerThree = new BatchEventProcessor[ValueEvent](rb, rb.newBarrier(consumerTwo.getSequence), handlerThree);

    rb.setGatingSequences(consumerThree.getSequence)

    val shooter = Shooter(config.iterations, rb, EventModule.fillEvent)

    playWith(List(consumerOne, consumerTwo, consumerThree)){
      shooter ! 'fire
      countDownLatch.await()
    }

  }
}