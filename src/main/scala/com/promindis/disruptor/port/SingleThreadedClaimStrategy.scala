package com.promindis.disruptor.port

import annotation.tailrec
import java.util.concurrent.locks.LockSupport
import Utils._
/**
 * Date: 08/02/12
 * Time: 15:58
 */

case class SingleThreadedClaimStrategy(bufferSize: Int) extends ClaimStrategy {

  val minGatingSequence = RSequence()
  val claimSequence = RSequence()

  @inline def sequence: Long = claimSequence.get()

  override def incrementAndGet(dependentSequences: Seq[RSequence]):Long =
    incrementAndGet(1L, dependentSequences)

  @inline override def incrementAndGet(delta: Long, dependentSequences: Seq[RSequence]): Long = {
    val newSequence = sequence + delta
    claimSequence.set(newSequence)
    waitForAvailableSlotAt(dependentSequences)
    newSequence
  }

  @inline def waitForAvailableSlotAt(sequences: Seq[RSequence]) {
    val wrapSequence = sequence - bufferSize

    @tailrec
    @inline def loopWaitingForAvailable(slot: Long): Long = {
      if (wrapSequence > slot) {
        LockSupport.parkNanos(1L)
        loopWaitingForAvailable(smallestSlotIn(sequences))
      }
      else slot
    }

    if (wrapSequence > minGatingSequence.get()) {
      minGatingSequence.set(loopWaitingForAvailable(smallestSlotIn(sequences)))
    }
  }

  @inline def updated(sequence: RSequence, value: Long) = {
    sequence.set(value)
    value
  }

  override def hasAvailableCapacity(expected: Long, sequences: Seq[RSequence]): Boolean = {
    val wrapSequence = sequence + expected - bufferSize
    if (wrapSequence > minGatingSequence.get())
      (updated(minGatingSequence, smallestSlotIn(sequences)) > wrapSequence)
    else
    true
  }

  def setSequence(value: Long, sequences: RSequence*) {
    claimSequence.set(value)
    waitForAvailableSlotAt(sequences)
  }

  def serialisePublishing(newValue: Long, sequence: RSequence, batchSize: Int)  {
    sequence.set(newValue)
  }
}
