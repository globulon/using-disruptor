package com.promindis.disruptor.support

object Utils {

  implicit def booleanToTernary(p: Boolean) = new {
    def ?[T](a: => T) = new {
      def |[T](b: => T) = if (p) a else b
    }
  }
}