package com.promindis.disruptor.configurations.diamond

import com.promindis.disruptor.adapters.EventModule._
import java.util.concurrent.CountDownLatch
import com.lmax.disruptor._
import com.promindis.disruptor.adapters.RingBufferFactory._
import com.promindis.disruptor.adapters.Shooter
import com.promindis.disruptor.configurations.{Configuration, Scenario}
import com.promindis.disruptor.support.StateMonad

/**
 * Reproduces LMAX diamond configuration
 */
object Diamond extends Scenario{

//  def join[T](handler: EventHandler[T], rb: RingBuffer[T]) = new StateMonad[Unit, Seq[(BatchEventProcessor[T], EventHandler[T])] ] {
//    def apply(list: Seq[(BatchEventProcessor[T], EventHandler[T])]) = {
//      list match {
//        case (p::ps)=>
//          val sequences = list.unzip._1.map{_.getSequence}
//          val newProcessor = new BatchEventProcessor[T](rb, rb.newBarrier(sequences: _*), handler)
//          rb.setGatingSequences(newProcessor.getSequence)
//          ((), (newProcessor, handler)::p::ps)
//        case _ =>
//          val newProcessor = new BatchEventProcessor[T](rb, rb.newBarrier(), handler)
//          rb.setGatingSequences(newProcessor.getSequence)
//          ((), (newProcessor, handler)::Nil)
//      }
//    }
//  }
  
  def challenge(implicit config: Configuration): Long = {

    val rb = ringBuffer(ValueEventFactory, config.ringBufferSize, new YieldingWaitStrategy());

    val countDownLatch = new CountDownLatch(1);
    val firstHandler = Handler("one")
    val secondHandler = Handler("two")
    val thirdHandler = Handler("three", latch = Some(countDownLatch), expectedShoot = config.iterations)

    val firstBarrier = rb.newBarrier();
    val consumerOne = new BatchEventProcessor[ValueEvent](rb, firstBarrier, firstHandler)
    val consumerTwo = new BatchEventProcessor[ValueEvent](rb, firstBarrier, secondHandler)
    
    val secondBarrier = rb.newBarrier(consumerOne.getSequence, consumerTwo.getSequence);
    val consumerThree = new BatchEventProcessor[ValueEvent](rb, secondBarrier, thirdHandler)

    rb.setGatingSequences(consumerThree.getSequence);

    val shooter = Shooter(config.iterations, rb, fillEvent)

    playWith (List(consumerOne, consumerTwo, consumerThree)){
      shooter ! 'fire
      countDownLatch.await();
    }

  }

}