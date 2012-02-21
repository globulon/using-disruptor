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

  override def waitFor(timeout: Long, sourceUnit: TimeUnit, sequence: Long, cursor: RSequence, barrier: SequencesBarrier) = {
      waitForSequence(COUNTER, OnlyForCusor(sequence, cursor), barrier, VanishingTime(interval = sourceUnit.toMillis(timeout)))
  }

  override def waitFor(timeout: Long, sourceUnit: TimeUnit, sequence: Long, barrier: SequencesBarrier, dependents: Seq[RSequence]) = {
      waitForSequence(COUNTER, ForDependencies(sequence, dependents), barrier, VanishingTime(interval = sourceUnit.toMillis(timeout)))
  }

  override def waitFor(sequence: Long, cursor: RSequence, barrier: SequencesBarrier) = {
      waitForSequence(COUNTER, OnlyForCusor(sequence, cursor), barrier)
  }

  override def waitFor(sequence: Long, barrier: SequencesBarrier, dependents: Seq[RSequence]) = {
      waitForSequence(COUNTER, ForDependencies(sequence, dependents), barrier)
  }

  @tailrec def waitForSequence(counter: Int, keepWaiting: WaitingCriteria, barrier: SequencesBarrier, time: VanishingTime): Option[Long] = {
    counter match {
      case _ if (time.overdue()) => keepWaiting.result()
      case _ if (barrier.alerted) => None
      case _ if (keepWaiting()) => waitForSequence(counter - 1, keepWaiting, barrier, time.reduce())
      case 0 =>
        Thread.`yield`()
        keepWaiting.result()
      case _ => keepWaiting.result()
    }
  }

  @tailrec def waitForSequence(counter: Int, keepWaiting: WaitingCriteria, barrier: SequencesBarrier): Option[Long] = {
    counter match {
      case _ if (barrier.alerted) => None
      case _ if (keepWaiting()) => waitForSequence(counter - 1, keepWaiting, barrier)
      case 0 =>
        Thread.`yield`()
        keepWaiting.result()
      case _ => keepWaiting.result()
    }
  }

}

object YieldingWaitStrategy {
 val COUNTER = 100

  def apply() = new YieldingWaitStrategy()
}