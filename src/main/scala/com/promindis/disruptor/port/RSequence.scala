package com.promindis.disruptor.port
import Sequences._
import java.util.concurrent.atomic.AtomicLong

/**
 * Date: 08/02/12
 * Time: 11:55
 * @todo Padding will require sampling on that class size
 */

case class RSequence(updater: AtomicLong) {
  protected[RSequence] var test: Long = 0

  def updated(newValue: Long) = {
    set(newValue)
    this
  }

  def set(value: Long) {updater.lazySet(value)}

  def get() = updater.get()

  def compareAndSet(expectedValue: Long, toValue: Long) = updater.compareAndSet(expectedValue, toValue)


}

object RSequence {

  def apply(value: Long = INITIAL_VALUE) = new RSequence(new AtomicLong(value))
}
