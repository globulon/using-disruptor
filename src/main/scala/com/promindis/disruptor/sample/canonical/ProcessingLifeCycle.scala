package com.promindis.disruptor.sample.canonical

import com.lmax.disruptor.BatchEventProcessor
import actors.Actor
import java.util.concurrent.Executors._


/**
 * User: omadas
 * Date: 23/01/12
 * Time: 12:40
 * Manages the event processor life cycle
 * An event processor must be started
 * so it must halted
 * No doing that will keep the thread in the pool
 * hanging
 */

class ProcessingLifeCycle[T](val eventProcessor: BatchEventProcessor[T]) extends Actor {
  self =>
  val executor = newSingleThreadExecutor()

  def act {
    react {
      case 'start =>
        println("Starting processing...")
        executor.submit(eventProcessor)
        reply("started")
        act
      case 'stop =>
        println("Stopping Starting processing...")
        eventProcessor.halt()
        executor.shutdown()
        println("Exiting processing...")
        self.exit()
      case _ =>
        println("Trapped in processing...")
        self ! 'stop
        act
    }
  }
}

object ProcessingLifeCycle {
  def apply[T](eventProcessor: BatchEventProcessor[T]) = {
    val manager = new ProcessingLifeCycle(eventProcessor)
    manager.start()
    (manager !! 'start)()
    manager
  }
}