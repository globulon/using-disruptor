package com.promindis.disruptor.port

import collection.mutable.ArrayBuffer

/**
 * Date: 18/02/12
 * Time: 19:29
 * The structure in the ring buffer seems to represent a bottleneck
 * Array Buffer improves better than Vector.
 * We should identify a suitable indexed immutable structure
 */
final class RingBuffer[T](val factory: EventFactory[T], claimStrategy: ClaimStrategy, waitStrategy: WaitStrategy)
  extends Sequencer(claimStrategy, waitStrategy){
  val eventBuckets = ArrayBuffer.fill(claimStrategy.bufferSize){factory()}
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
