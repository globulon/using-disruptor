package com.promindis.disruptor.port

import actors.threadpool.TimeUnit


/**
 * Date: 10/02/12
 * Time: 17:21
 */

trait WaitStrategy {
  def signalAllWhenBlocking() {}

  def waitFor( sequence: Long,
               cursor: RSequence,
               barrier: SequencesBarrier ): Option[Long]


  def waitFor( sequence: Long,
               barrier: SequencesBarrier ,
               dependents: Seq[RSequence]): Option[Long]

  def waitFor(timeout: Long,
              sourceUnit: TimeUnit,
              sequence: Long,
              cursor: RSequence,
              barrier: SequencesBarrier ): Option[Long]


  def waitFor(timeout: Long,
               sourceUnit: TimeUnit,
               sequence: Long,
               barrier: SequencesBarrier ,
               dependents: Seq[RSequence]): Option[Long]

//  def signalAllWhenBlocking: Unit
}
