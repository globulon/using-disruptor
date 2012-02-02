package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicBoolean
import com.lmax.disruptor._
import com.promindis.disruptor.adapters.Processor
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

  def alert: PartialFunction[Throwable, Option[Long]] = {
    case ex: AlertException if (!running.get())=> None
  }

  def trap[E](sequence: Long, forEvent: E): PartialFunction[Throwable, Option[Long]] = {
    case ex: Throwable =>
      print("exception raised")
      exceptionHandler.handleEventException(ex, sequence, forEvent)
      Some(sequence)
  }

  def managing(trap: => PartialFunction[Throwable, Option[Long]])(f: => Long): Option[Long] = {
    try {
      Some(f)
    } catch trap
  }

  def nextSequence(l: Long) = {
    managing (alert){
      sequenceBarrier.waitFor(l)
    }
  }

  def handlingEvent(sequence: Long, asLast: Boolean): Option[Long] = {
    val event = ringBuffer.get(sequence)
    managing(alert orElse trap(sequence, event)) {
      eventHandler.onEvent(event, sequence, asLast)
      sequence
    }
  }

  def handleEvents(nextSequence: Long, availableSequence: Long): Option[Long]  = {
    Some((nextSequence to availableSequence).dropWhile{ index =>
      handlingEvent(index, index == availableSequence).isDefined
    }.headOption.getOrElse(availableSequence))
  }

  def loop() {

    def processEvents(fromIndex: Long) = {
      for {
        availableSequence <- nextSequence(fromIndex);
        nextIndex <- handleEvents(fromIndex, availableSequence)
      } yield nextIndex
    }

    @tailrec def innerLoop(fromIndex: Long) {
      val lastIndex = processEvents(fromIndex)
      if (lastIndex.isDefined)  {
        sequence.set(lastIndex.get)
        innerLoop(lastIndex.get + 1)
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