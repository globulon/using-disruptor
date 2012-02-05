package com.promindis.disruptor.adapters

import com.lmax.disruptor.RingBuffer
import actors.Actor
import actors.scheduler.DaemonScheduler

class Shooter[T](numberOfShoot: Long, val ringBuffer: RingBuffer[T], val eventStrategy: T => T) extends Actor {
  self =>
  override def scheduler = DaemonScheduler
  implicit def adapted[T](ringBuffer: RingBuffer[T]) = new {

    def shoot(update: T => T) {
      val (sequence, event) = nextEventStructure(ringBuffer)
      update(event)
      ringBuffer.publish(sequence);
    }

    def nextEventStructure[T](rb: RingBuffer[T]): (Long, T) = {
      val sequence = rb.next();
      (sequence, rb.get(sequence));
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

