package com.promindis.disruptor.port

import actors.threadpool.TimeUnit

/**
 * Date: 10/02/12
 * Time: 17:25
 */

case class FakeWaitStrategy() extends WaitStrategy{
  var waitedSequence = -1L

  var inputCursor : RSequence = _

  def waitFor(sequence: Long, barrier: SequencesBarrier, dependents: Seq[RSequence]):Option[Long] = {
    waitFor(sequence, null, barrier)
  }

  def waitFor(timeout: Long, sourceUnit: TimeUnit, sequence: Long, barrier: SequencesBarrier, dependents: Seq[RSequence]) =
    waitFor(sequence, barrier , dependents)

  def waitFor(sequence: Long, cursor: RSequence, barrier: SequencesBarrier) = {
    waitedSequence = sequence
    inputCursor = cursor
    Some(sequence)

  }

  def waitFor(timeout: Long, sourceUnit: TimeUnit, sequence: Long, cursor: RSequence, barrier: SequencesBarrier) = {
    waitFor(sequence, cursor, barrier)
  }
}
