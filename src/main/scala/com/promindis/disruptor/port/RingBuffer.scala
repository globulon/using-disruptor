package com.promindis.disruptor.port

/**
 * Date: 18/02/12
 * Time: 19:29
 */

final class RingBuffer[T](val factory: EventFactory[T], claimStrategy: ClaimStrategy, waitStrategy: WaitStrategy)
  extends Sequencer(claimStrategy, waitStrategy){
  val eventBuckets = Vector.fill(claimStrategy.bufferSize){factory()}
  val mask = claimStrategy.bufferSize - 1


  def get(index: Long): T = {
    eventBuckets((index & mask).toInt)
  }

}

object RingBuffer {
  def apply[T](factory: EventFactory[T] ,claimStrategy: ClaimStrategy,waitStrategy: WaitStrategy):  Option[RingBuffer[T]]=
    if (Integer.bitCount(claimStrategy.bufferSize) == 1)
      Some(new RingBuffer[T](factory, claimStrategy, waitStrategy))
    else None

}
