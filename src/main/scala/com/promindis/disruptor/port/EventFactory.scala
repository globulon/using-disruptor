package com.promindis.disruptor.port

/**
 * Date: 18/02/12
 * Time: 19:25
 */

trait EventFactory[T] {
  def apply(): T
}
