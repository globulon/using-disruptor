package com.promindis.disruptor.port

import actors.threadpool.TimeUnit
import annotation.tailrec
import Utils._
import collection.Seq

/**
 * Date: 11/02/12
 * Time: 15:14
 * Need refactoring: we have room for a pure strategy pattern here
 */
final class YieldingWaitStrategy() extends WaitStrategy {
  import YieldingWaitStrategy._

  def waitFor(timeout: Long, sourceUnit: TimeUnit, sequence: Long, cursor: RSequence, barrier: SequencesBarrier, dependents: RSequence*) = {
    for {
      result <- waitForSequence(COUNTER, strategyFor(sequence, cursor, dependents), barrier.alerted, VanishingTime(interval = sourceUnit.toMillis(timeout)))
    } yield result
  }

  def waitFor(sequence: Long, cursor: RSequence, barrier: SequencesBarrier, dependents: RSequence*) = {
    for {
      result <- waitForSequence(COUNTER, strategyFor(sequence, cursor, dependents), barrier.alerted)
    } yield result
  }

  @tailrec def waitForSequence(counter: Int, keepWaiting: WaitingStrategy, alerted: => Boolean, time: VanishingTime): Option[Long] = {
    counter match {
      case _ if (time.overdue()) => keepWaiting.result()
      case _ if (alerted) => None
      case _ if (keepWaiting()) => waitForSequence(counter - 1, keepWaiting, alerted, time.reduce())
      case 0 =>
        Thread.`yield`()
        keepWaiting.result()
      case _ => keepWaiting.result()
    }
  }

  @tailrec def waitForSequence(counter: Int, keepWaiting: WaitingStrategy, alerted: => Boolean): Option[Long] = {
    counter match {
      case _ if (alerted) => None
      case _ if (keepWaiting()) => waitForSequence(counter - 1, keepWaiting, alerted)
      case 0 =>
        Thread.`yield`()
        keepWaiting.result()
      case _ => keepWaiting.result()
    }
  }

  @inline def strategyFor(sequence: Long, cursor: RSequence, dependencies: Seq[RSequence]): WaitingStrategy = {
    if (dependencies.size == 0)
      WaitOnlyForCusor(sequence, cursor)
    else
      WaitForDependencies(sequence, dependencies)
  }
}
 trait WaitingStrategy {
   def apply(): Boolean
   def result(): Option[Long]
 }

final case class WaitOnlyForCusor(sequence: Long, cursor: RSequence) extends WaitingStrategy{
  override def apply() = cursor.get() < sequence
  override def result() = Some(cursor.get())
}

final case class WaitForDependencies(sequence: Long, dependencies: Seq[RSequence]) extends WaitingStrategy{
  override def apply() = smallestSlotIn(dependencies) < sequence
  override def result() = Some(smallestSlotIn(dependencies))
}


object YieldingWaitStrategy {
 val COUNTER = 100

  def apply() = new YieldingWaitStrategy()
}