package com.promindis.disruptor.port

import actors.threadpool.TimeUnit

/**
 * Date: 10/02/12
 * Time: 17:14
 */

sealed trait SequencesBarrier {
  private var alertOn = false

  def cursorValue: Long

  def alerted = alertOn

  def doAlert() {alertOn = true}

  def clearAlert() {alertOn = false}

  def waitFor(sequence: Long) : Option[Long]

  def waitFor(duration: Long, units: TimeUnit, sequence: Long) : Option[Long]
}

object SequencesBarrier {
  def apply(waitStrategy: WaitStrategy,
            cursor: RSequence,
            dependentSequences: RSequence*) =

    if (dependentSequences.size == 0)
    ProcessingBarrierWithNoDependencies(waitStrategy, cursor)
  else
    ProcessingSequencesBarrier(waitStrategy, cursor, dependentSequences)

}

protected final case class
ProcessingBarrierWithNoDependencies( waitStrategy: WaitStrategy,
                                     cursor: RSequence) extends SequencesBarrier{

  override def waitFor(sequence: Long): Option[Long] = {
    if (alerted) None
    else waitStrategy.waitFor(sequence, cursor, this)
  }

  override def waitFor(duration: Long, units: TimeUnit, sequence: Long) = {
    if (alerted) None
    else waitStrategy.waitFor(duration, units, sequence, cursor, this)
  }

  def cursorValue = cursor.get()
}

protected final case class ProcessingSequencesBarrier (waitStrategy: WaitStrategy,
                                       cursor: RSequence,
                                       dependentSequences: Seq[RSequence]
) extends SequencesBarrier {

  override def waitFor(sequence: Long): Option[Long] = {
    if (alerted) None
    else waitStrategy.waitFor(sequence, this,  dependentSequences)
  }

  override def waitFor(duration: Long, units: TimeUnit, sequence: Long) = {
    if (alerted) None
    else waitStrategy.waitFor(duration, units, sequence, this,  dependentSequences)
  }

  def cursorValue = cursor.get()
}
