package com.promindis.disruptor

import com.lmax.disruptor.{BatchEventProcessor, SleepingWaitStrategy, SingleThreadedClaimStrategy, RingBuffer}


object UseEventModule {
//  import EventModule._
//
//  def RING_SIZE: Int = 2048
//
//  def main(arguments: Array[String]) {
//    val ringBuffer =
//      new RingBuffer[ValueEvent](EventFactory,
//        new SingleThreadedClaimStrategy(RING_SIZE),
//        new SleepingWaitStrategy());
//
//    val barrier = ringBuffer.newBarrier();
//    val eventProcessor = new BatchEventProcessor[ValueEvent](barrier, eventHandler);
//    ringBuffer.setGatingSequences(eventProcessor.getSequence);
//
//    // Each EventProcessor can run on a separate thread
//    EXECUTOR.submit(eventProcessor);
//  }
}