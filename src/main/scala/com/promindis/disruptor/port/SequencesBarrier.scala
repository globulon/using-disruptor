package com.promindis.disruptor.port

import actors.threadpool.TimeUnit

/**
 * Date: 10/02/12
 * Time: 17:14
 */

trait SequencesBarrier {
  private var alertOn = false

  def cursorValue: Long

  def alerted = alertOn

  def doAlert() {alertOn = true}

  def clearAlert() {alertOn = false}

  def waitFor(sequence: Long) : Option[Long]

  def waitFor(duration: Long, units: TimeUnit, sequence: Long) : Option[Long]
}

case class ProcessingSequencesBarrier (waitStrategy: WaitStrategy,
                                       cursor: RSequence,
                                       dependentSequences: RSequence*
) extends SequencesBarrier {

  override def waitFor(sequence: Long): Option[Long] = {
    if (alerted) None
    else waitStrategy.waitFor(sequence, cursor, this,  dependentSequences: _*)
  }

  override def waitFor(duration: Long, units: TimeUnit, sequence: Long) = {
    if (alerted) None
    else waitStrategy.waitFor(duration, units, sequence, cursor, this,  dependentSequences: _*)
  }

  def cursorValue = cursor.get()
}
