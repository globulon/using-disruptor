package com.promindis.disruptor.adapters

import actors.Actor
import actors.scheduler.DaemonScheduler
import com.promindis.disruptor.port.RingBuffer

class Shooter[T](numberOfShoot: Long, val ringBuffer: RingBuffer[T], val eventStrategy: T => T) extends Actor {
  self =>
  override def scheduler = DaemonScheduler
  implicit def adapted[T](ringBuffer: RingBuffer[T]) = new {

    def shoot(update: T => T) {
      val Some((sequence, event)) = nextEventStructure(ringBuffer)
      update(event)
      ringBuffer.publish(sequence);
    }

    def nextEventStructure[T](rb: RingBuffer[T]) = {
      for {
        sequence <- rb.next()
        bucket = rb.get(sequence)
      } yield (sequence, bucket)
    }
  }

  def act() {
    react {
      case 'fire =>
        for (i <- 1L to numberOfShoot) {
          ringBuffer.shoot(eventStrategy)
        }
        self.exit()
    }
  }
}

object Shooter {
  def apply[T](numberOfShoot: Long, ringBuffer: RingBuffer[T], fillEvent: T => T) =
    new Shooter(numberOfShoot, ringBuffer, fillEvent).start()
}

