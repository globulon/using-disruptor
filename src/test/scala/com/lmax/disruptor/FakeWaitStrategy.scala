package com.lmax.disruptor

import java.util.concurrent.TimeUnit

/**
 * Date: 10/02/12
 * Time: 16:41
 */

case class FakeWaitStrategy() extends WaitStrategy{
  var waitedSequence = -1L

  def signalAllWhenBlocking() {}

  def waitFor(sequence: Long, cursor: Sequence, dependents: Array[Sequence], barrier: SequenceBarrier) = {
    waitedSequence = sequence
    waitedSequence
  }

  def waitFor(sequence: Long, cursor: Sequence, dependents: Array[Sequence], barrier: SequenceBarrier, timeout: Long, sourceUnit: TimeUnit) =
    waitFor(sequence  , cursor  , dependents , barrier)
}