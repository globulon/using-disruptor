package com.promindis.disruptor.port

/**
 * Date: 08/02/12
 * Time: 14:23
 * @todo eveluate overhead of getter methods
 */

final class PaddedLong(var value: Long) extends Mutable{
  @volatile private var p1 = 0
  @volatile private var p2 = 0
  @volatile private var p3 = 0
  @volatile private var p4 = 0
  @volatile private var p5 = 0
  @volatile private var p6 = 0
  @volatile private var p7 = 7L

  def sumPaddingToPreventOptimisation() = p1 + p2 + p3 + p4 + p5 + p6 + p7
}

object  PaddedLong {
  def apply(value: Long = 0L) = new PaddedLong(value)
}

