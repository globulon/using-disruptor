package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicBoolean
import com.lmax.disruptor._
import com.promindis.disruptor.adapters.Processor
import collection.immutable.NumericRange
import annotation.tailrec

final case class BatchEventProcessor[T](ringBuffer: RingBuffer[T], sequenceBarrier: SequenceBarrier, eventHandler: EventHandler[T])
  extends Processor with EventProcessor {
  val running: AtomicBoolean = new AtomicBoolean(false)
  val exceptionHandler: ExceptionHandler = new FatalExceptionHandler()
  val sequence: Sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE)


  override def halt() {
    stopRunning()
    sequenceBarrier.alert()
  }


  def safely[T](f: => T) = {
    try {
      Some(f)
    } catch {
      case ex: AlertException if (!running.get())=>
        println("alsert stop")
      None
    }
  }

  def nextSequence(l: Long) = {
    safely{
      sequenceBarrier.waitFor(l)
    }
  }

  def eventHandling(nextSequence: Long, availableSequence: Long): Option[Long]  = {

    NumericRange.inclusive(nextSequence, availableSequence, 1L).foreach { step =>
      val event = Some(ringBuffer.get(nextSequence))
      eventHandler.onEvent(event.get, nextSequence, nextSequence == availableSequence)
    }
    Some(availableSequence)

//    catch {
//      case ex: Throwable => {
//        println("exception")
//        exceptionHandler.handleEventException(ex, nextSequence, event)
//        sequence.set(nextSequence)
//        nextSequence += 1
//      }
//    }

  }

  def loop() {

    def processEvents(index: Long) = {
      for {
        availableSequence <- nextSequence(index);
        nextIndex <- eventHandling(index, availableSequence)
      } yield nextIndex
    }

    @tailrec def innerLoop(fromIndex: Long) {
      val nextIndex = processEvents(fromIndex)
      if (nextIndex.isDefined)  {
        sequence.set(nextIndex.get)
        innerLoop(nextIndex.get + 1)
      }
    }

    innerLoop(sequence.get + 1L)
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