package com.promindis.disruptor.adapters

import actors.Actor
import actors.scheduler.DaemonScheduler
import com.promindis.disruptor.port.RingBuffer

class Shooter[T](numberOfShoot: Long, val ringBuffer: RingBufferOnSteroids[T], val eventStrategy: T => T) extends Actor {
  self =>
  override def scheduler = DaemonScheduler

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

/**
 * We use a complete case class instead of Kestrel combinators
 * (invoked by reflection)
 * @param ringBuffer ring buffer
 * @tparam T event type
 */
final case class RingBufferOnSteroids[T](ringBuffer: RingBuffer[T]) {

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

object Shooter {
  def apply[T](numberOfShoot: Long, ringBuffer: RingBuffer[T], fillEvent: T => T) =
    new Shooter(numberOfShoot, RingBufferOnSteroids(ringBuffer), fillEvent).start()
}

