package com.promindis.disruptor

import java.util.concurrent.Callable
import java.util.concurrent.Executors._

/**
 * Date: 09/02/12
 * Time: 12:55
 */

object Tools {
  def submit[T](callable: => Callable[T]) = {
    val pool = newSingleThreadExecutor()
    try {
      pool.submit(callable)
    } finally {
      pool.shutdown()
    }
  }

  def submitFragment[R](fragment: => R) = {
    val pool = newSingleThreadExecutor()
    try {
      pool.submit(new Callable[R] {
        def call = fragment
      })
    } finally {
      pool.shutdown()
    }
  }


}
