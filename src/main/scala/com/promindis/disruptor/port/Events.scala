package com.promindis.disruptor.port


object Events {

 trait EventHandler[T] {
    def onEvent(event: T, sequence: Long, endOfBatch: Boolean)
 }

}