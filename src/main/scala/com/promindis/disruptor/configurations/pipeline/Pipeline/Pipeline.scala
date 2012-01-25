package com.promindis.disruptor.configurations.pipeline.Pipeline

import com.promindis.disruptor.adapters.RingBufferFactory._
import java.util.concurrent.CountDownLatch
import com.lmax.disruptor.BatchEventProcessor

import com.promindis.disruptor.adapters.EventModule.{Handler, ValueEvent, ValueEventFactory}
import com.promindis.disruptor.adapters.{Shooter, EventModule}
import com.promindis.disruptor.configurations.{Configuration, Scenario}


object Pipeline extends Scenario{

  def challenge(implicit config: Configuration) = {
    val rb = ringBuffer(ValueEventFactory,size = config.ringBufferSize);

    val countDownLatch = new CountDownLatch(1)
    val barrier =  rb.newBarrier()
    val handlerOne = Handler("P1")
    val consumerOne = new BatchEventProcessor[ValueEvent](rb, barrier, handlerOne);

    val handlerTwo = Handler("P2")
    val consumerTwo = new BatchEventProcessor[ValueEvent](rb, rb.newBarrier(consumerOne.getSequence), handlerTwo);

    val handlerThree = Handler("P3", latch = Some(countDownLatch), expectedShoot = config.iterations)
    val consumerThree = new BatchEventProcessor[ValueEvent](rb, rb.newBarrier(consumerTwo.getSequence), handlerThree);

    rb.setGatingSequences(consumerThree.getSequence)

    val shooter = Shooter(config.iterations, rb, EventModule.fillEvent)

    playWith(List(consumerOne, consumerTwo, consumerThree)){
      shooter ! 'fire
      countDownLatch.await()
    }

  }

  def main(args: Array[String]) { run(Configuration())}
}