package com.promindis.disruptor.port

import actors.threadpool.TimeUnit
import Utils._
import annotation.tailrec

/**
 * Date: 15/02/12
 * Time: 16:03
 * @todo Option[Long] for next operation should be replaced by Either...
 */

class Sequencer(val claimStrategy: ClaimStrategy, val waitStrategy: WaitStrategy) {

  private var gatingSequences: Option[Seq[RSequence]] = None

  private val cursor = RSequence()

  def bufferSize = claimStrategy.bufferSize

  def cursorValue = cursor.get()

  def next(): Option[Long] = {
    if (gatingSequences.isDefined)
      Some(claimStrategy.incrementAndGet(gatingSequences.get))
    else
      None
  }

  def next(timeout: Long, unit: TimeUnit): Option[Long] = {
    for {
          sequences <- gatingSequences
        _ <- waitForAvailable(1, timeout, unit, sequences: _*)
    }
    yield claimStrategy.incrementAndGet(sequences)
  }

  def next(descriptor: BatchDescriptor): Option[BatchDescriptor] = {
    if (gatingSequences.isDefined)
      Some(descriptor.withEnd(claimStrategy.incrementAndGet(descriptor.size, gatingSequences.get)))
    else
      None
  }

  def next(descriptor: BatchDescriptor, timeout: Long, unit: TimeUnit): Option[BatchDescriptor] = {
    for {
      sequences <- gatingSequences
      _ <- waitForAvailable(descriptor.size, timeout, unit, sequences: _*)
    } yield descriptor.withEnd(claimStrategy.incrementAndGet(descriptor.size, sequences))

  }

  def withGating(sequences: RSequence*) = {
    gatingSequences = Some(sequences)
    this
  }

  def batchDescriptor(size: Int): BatchDescriptor =
  BatchDescriptor(if (size < bufferSize) size else bufferSize)

  def barrierFor(sequences: RSequence*): SequencesBarrier =
    SequencesBarrier(waitStrategy, cursor, sequences: _*)

  def barrier = SequencesBarrier(waitStrategy, cursor)

  def waitForAvailable(value: Long, timeout: Long, unit: TimeUnit, sequences: RSequence*): Option[Long] = {
    @tailrec def loopWaiting(timeSlice: VanishingTime): Option[Long] = {
      timeSlice match {
        case _ if claimStrategy.hasAvailableCapacity(value, sequences) => Some(value)
        case _ if timeSlice.overdue() => None
        case _ => loopWaiting(timeSlice.reduce())
      }
    }
    loopWaiting(VanishingTime(interval = unit.toMillis(timeout)))
  }


  def forcePublish(value: Long) {
    cursor.set(value)
    waitStrategy.signalAllWhenBlocking();
  }

  def publish(sequence: Long) {
    publish(sequence, 1);
  }

  def publish(batchDescriptor: BatchDescriptor) {
    publish(batchDescriptor.end, batchDescriptor.size)
  }

  def publish(sequence: Long, batchSize: Long) {
    claimStrategy.serialisePublishing(sequence, cursor, batchSize);
    waitStrategy.signalAllWhenBlocking();
  }

}

object Sequencer {
  def apply(claimStrategy: ClaimStrategy, waitStrategy: WaitStrategy) =
    new Sequencer(claimStrategy, waitStrategy)
}
