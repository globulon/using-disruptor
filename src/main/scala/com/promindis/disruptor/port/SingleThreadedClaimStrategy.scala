package com.promindis.disruptor.port

import annotation.tailrec
import java.util.concurrent.locks.LockSupport
import Long._

/**
 * Date: 08/02/12
 * Time: 15:58
 */

case class SingleThreadedClaimStrategy(bufferSize: Long) extends ClaimStrategy {

  val minGatingSequence = RSequence()
  val claimSequence = RSequence()

  @inline def sequence: Long = claimSequence.get()

  def incrementAndGet(dependentSequences: RSequence*):Long =
    incrementAndGet(1L, dependentSequences: _*)

  def incrementAndGet(delta: Long, dependentSequences: RSequence*): Long = {
    val newSequence = sequence + delta
    claimSequence.set(newSequence)
    waitForAvailableSlotAt(dependentSequences: _*)
    newSequence
  }


  @inline def smallestSlotIn(sequences: Seq[RSequence]): Long = {
    sequences.foldLeft(MaxValue) {
      (cur, item) =>
        if (item.get() < cur) item.get() else cur}
  }

  def waitForAvailableSlotAt(sequences: RSequence*) {
    val wrapSequence = sequence - bufferSize

    @tailrec
    def loopWaitingForAvailable(slot: Long): Long = {
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

  def hasAvailableCapacity(expected: Int, sequences: RSequence*): Boolean = {
    val wrapSequence = sequence + 1L - bufferSize
    if (wrapSequence > minGatingSequence.get())
                           (updated(minGatingSequence, smallestSlotIn(sequences)) > wrapSequence)
    else
    true
  }

  def setSequence(value: Long, sequences: RSequence*) {
    claimSequence.set(value)
    waitForAvailableSlotAt(sequences: _*)
  }

  def serialisePublishing(newValue: Long, sequence: RSequence, batchSize: Int)  {
    sequence.set(newValue)
  }
}
