package com.promindis.disruptor.support

trait StateMonad[+T, S]  {
  owner =>
  def apply(state: S): (T, S)

  def flatMap[U](f: T => StateMonad[U,S]) = new StateMonad[U, S] {
    override def apply(state: S) = {
      val (a, y) =  owner(state)
      f(a)(y)
    }
  }

  def map[U](f: T => U) = new StateMonad[U, S] {
    def apply(state: S) = {
      val (a, y) =  owner(state)
      (f(a), y)
    }
  }
}

object StateMonad {
  def apply[T, S](value: T) = new StateMonad[T, S] {
    def apply(state: S) = (value, state)
  }
}

