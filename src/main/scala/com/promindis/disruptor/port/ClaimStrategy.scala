package com.promindis.disruptor.port

/**
 * Date: 08/02/12
 * Time: 16:01
 */

trait ClaimStrategy {
  def serialisePublishing(sequence: Long, cursor: RSequence, batchSize: Long) {
      cursor.set(sequence)
  }

  def incrementAndGet(seq: Seq[RSequence]): Long

  def incrementAndGet(delta: Long, dependentSequences: Seq[RSequence]): Long

  def bufferSize: Int

  def hasAvailableCapacity(expected: Long, sequences: Seq[RSequence]): Boolean
}
