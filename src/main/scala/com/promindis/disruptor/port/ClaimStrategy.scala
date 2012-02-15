package com.promindis.disruptor.port

/**
 * Date: 08/02/12
 * Time: 16:01
 */

trait ClaimStrategy {
  def incrementAndGet(seq: RSequence*): Long

  def bufferSize: Long

  def hasAvailableCapacity(expected: Long, sequences: RSequence*): Boolean
}
