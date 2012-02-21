package com.promindis.disruptor

package object port {
  implicit val comparator = new  Ordering[RSequence] {
    val comparator = implicitly[Ordering[Long]]
    def compare(x: RSequence, y: RSequence) = {
      comparator.compare(x.get(), y.get())
    }
  }
}
