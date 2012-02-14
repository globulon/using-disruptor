package com.promindis.disruptor.port
import Sequences._
/**
 * Date: 14/02/12
 * Time: 20:36
 */

final case class BatchDescriptor(size: Long, end: Long = INITIAL_VALUE) {

  def start = end - (size - 1)

  def withEnd(newEnd: Long) = copy(end = newEnd)

}
