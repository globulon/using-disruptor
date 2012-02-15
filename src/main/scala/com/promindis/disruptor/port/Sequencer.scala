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

  def next(descriptor: BatchDescriptor): Option[BatchDescriptor] = {
    for {sequences <- gatingSequences}
      yield descriptor.withEnd(claimStrategy.incrementAndGet(descriptor.size, sequences: _*))
  }

  def next(descriptor: BatchDescriptor, timeout: Long, unit: TimeUnit): Option[BatchDescriptor] = {
    val result = (for {
      sequences <- gatingSequences
      _ <- waitForAvailable(descriptor.size, timeout, unit, sequences: _*)
    } yield claimStrategy.incrementAndGet(descriptor.size, sequences: _*))

    println("result" + result)
    for {data <- result} yield descriptor.withEnd(data)
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
    println("waitForAvailable " + value)
    @tailrec def loopWaiting(timeSlice: VanishingTime): Option[Long] = {
      println(claimStrategy.hasAvailableCapacity(value, sequences: _*))
      println(timeSlice)
      timeSlice match {
        case _ if claimStrategy.hasAvailableCapacity(value, sequences: _*) => Some(value)
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

  def publish(sequence: Long, batchSize: Long)
  {
    claimStrategy.serialisePublishing(sequence, cursor, batchSize);
    waitStrategy.signalAllWhenBlocking();
  }

}

object Sequencer {
  def apply(claimStrategy: ClaimStrategy, waitStrategy: WaitStrategy) =
    new Sequencer(claimStrategy, waitStrategy)
}
