package com.promindis.disruptor.configurations.unicast

import com.promindis.disruptor.adapters.RingBufferFactory._
import com.lmax.disruptor.BatchEventProcessor
import com.promindis.disruptor.adapters.EventModule.{Handler, ValueEvent, ValueEventFactory}
import java.util.concurrent.CountDownLatch
import com.promindis.disruptor.adapters.{EventModule, Shooter}
import com.promindis.disruptor.configurations.Scenario


object UnicastWithShooter extends Scenario{
  val RING_BUFFER_SIZE = 1024 * 1024
  val ITERATIONS= 1000L * 1000L * 50L
  val RUNS = 5

  def challenge() = {
    val rb = ringBuffer(ValueEventFactory,size = RING_BUFFER_SIZE);

    val barrier =  rb.newBarrier()
    val countDownLatch = new CountDownLatch(1)
    val handler = Handler("P1", latch = Some(countDownLatch), expectedShoot = ITERATIONS)
    val withProcessor = new BatchEventProcessor[ValueEvent](rb, barrier, handler);
    rb.setGatingSequences(withProcessor.getSequence)

    val shooter = Shooter(ITERATIONS, rb, EventModule.fillEvent)

    play{
      shooter ! 'fire
      countDownLatch.await()
    }(withProcessor)

  }

  def main(args: Array[String]) {
    for (_ <- 1 to RUNS) {
      println("Nb Op/s: " + challenge())
    }
  }
}