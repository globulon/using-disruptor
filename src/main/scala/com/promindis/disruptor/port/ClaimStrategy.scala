package com.promindis.disruptor.port

/**
 * Date: 08/02/12
 * Time: 16:01
 */

trait ClaimStrategy {
  def serialisePublishing(sequence: Long, cursor: RSequence, batchSize: Long) {
      cursor.set(sequence)
  }

  def incrementAndGet(seq: RSequence*): Long

  def incrementAndGet(delta: Long, dependentSequences: RSequence*): Long

  def bufferSize: Long

  def hasAvailableCapacity(expected: Long, sequences: RSequence*): Boolean
}
