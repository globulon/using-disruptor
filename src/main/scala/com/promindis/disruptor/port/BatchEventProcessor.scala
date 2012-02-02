package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicBoolean
import com.lmax.disruptor._
import annotation.tailrec
import com.promindis.disruptor.adapters.Processor
import collection.immutable.NumericRange
import com.promindis.disruptor.adapters.EventModule.ValueEvent

final case class BatchEventProcessor[T](ringBuffer: RingBuffer[T], sequenceBarrier: SequenceBarrier, eventHandler: EventHandler[T])
  extends Processor with EventProcessor {
  val running: AtomicBoolean = new AtomicBoolean(false)
  val exceptionHandler: ExceptionHandler = new FatalExceptionHandler()
  val sequence: Sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE)


  override def halt() {
    stopRunning()
    sequenceBarrier.alert()
  }


  def loop() {
    var nextSequence: Long = sequence.get + 1L
    var event: Option[T] = None
    var goOn = true
    while (goOn) {
      try {
        val availableSequence: Long = sequenceBarrier.waitFor(nextSequence)
        NumericRange.inclusive(nextSequence, availableSequence, 1L).foreach { step =>
          val event = Some(ringBuffer.get(nextSequence))
          eventHandler.onEvent(event.get, nextSequence, nextSequence == availableSequence)
          nextSequence + 1
        }
        sequence.set(availableSequence)
      } catch {
        case ex: AlertException if (!running.get())=>
            goOn = false
            println("alsert stop")
        case ex: Throwable => {
          println("exception")
          exceptionHandler.handleEventException(ex, nextSequence, event)
          sequence.set(nextSequence)
            nextSequence += 1
        }
      }
    }
  }

  def stopRunning() {
    running.set(false)
  }

  override def run() {
    if (running.compareAndSet(false, true)) {
      sequenceBarrier.clearAlert()
      loop()
      stopRunning()
    }
  }


  def getSequence = sequence
}

object BatcEventProcessor {
  def apply[T](ringBuffer: RingBuffer[T], sequenceBarrier: SequenceBarrier, eventHandler: EventHandler[T]) =
    new BatchEventProcessor[T](ringBuffer, sequenceBarrier, eventHandler)
}