package com.promindis.disruptor.port

import actors.threadpool.TimeUnit
import Utils._
import annotation.tailrec

/**
 * Date: 15/02/12
 * Time: 16:03
 * @todo Option[Long] for next operation should be replaced by Either...
 */

final class Sequencer(val claimStrategy: ClaimStrategy, val waitStrategy: WaitStrategy) {
  private var gatingSequences: Option[Seq[RSequence]] = None

  private val cursor = RSequence()

  def bufferSize = claimStrategy.bufferSize

  def cursorValue = cursor.get()

  def next(): Option[Long] = {
    for {sequences <- gatingSequences}
      yield claimStrategy.incrementAndGet(sequences: _*)
  }

  def next(timeout: Long, unit: TimeUnit): Option[Long] = {
    for {
      sequences <- gatingSequences
      _ <- waitForAvailable(1, timeout, unit, sequences: _*)
    }
    yield claimStrategy.incrementAndGet(sequences: _*)
  }

  def withGating(sequences: RSequence*) = {
    gatingSequences = Some(sequences)
    this
  }

  def batchDescriptor(size: Int): BatchDescriptor =
    BatchDescriptor(if (size < bufferSize) size else bufferSize)

  def barrierFor(sequences: RSequence*): SequencesBarrier =
    ProcessingSequencesBarrier(waitStrategy, cursor, sequences: _*)

  def waitForAvailable(value: Long, timeout: Long, unit: TimeUnit, sequences: RSequence*): Option[Long] = {

    @tailrec def loopWaiting(timeSlice: VanishingTime): Option[Long] = {
      timeSlice match {
        case _ if claimStrategy.hasAvailableCapacity(value, sequences: _*) => Some(value)
        case _ if timeSlice.overdue() => None
        case _ => loopWaiting(timeSlice.reduce())
      }
    }

    loopWaiting(VanishingTime(interval = unit.toMillis(timeout)))
  }
}

object Sequencer {
  def apply(claimStrategy: ClaimStrategy, waitStrategy: WaitStrategy) =
    new Sequencer(claimStrategy, waitStrategy)
}
