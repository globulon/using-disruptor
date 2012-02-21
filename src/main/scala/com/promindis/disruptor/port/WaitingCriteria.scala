package com.promindis.disruptor.port

import com.promindis.disruptor.port.Utils._

/**
 * Date: 21/02/12
 * Time: 10:04
 * Applicable strategy depending on whether
 * we only wait for a cursor or dependencies
 * Too many concerns in the same class smell
 */

trait WaitingCriteria {
  /**
   * @return true if one must wait to get info
   */
  def apply(): Boolean

  /**
   * @return expected result
   */
  def result(): Option[Long]
}


final case class WaitOnlyForCusor(sequence: Long, cursor: RSequence) extends WaitingCriteria{
  override def apply() = cursor.get() < sequence
  override def result() = Some(cursor.get())
}

final case class WaitForDependencies(sequence: Long, dependencies: Seq[RSequence]) extends WaitingCriteria{
  override def apply() = smallestSlotIn(dependencies) < sequence
  override def result() = Some(smallestSlotIn(dependencies))
}