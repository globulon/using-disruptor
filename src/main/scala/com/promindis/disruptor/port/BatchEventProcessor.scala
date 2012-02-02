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

  def safely[T](f: => T) = {
    try {
      Some(f)
    } catch {
      case ex: AlertException if (!running.get())=> None
    }
  }

  def nextSequence(l: Long) = {
    safely{
      sequenceBarrier.waitFor(l)
    }
  }

  def handle(nextSequence: Long, asLast: Boolean) {
    val event = ringBuffer.get(nextSequence)
    eventHandler.onEvent(event, nextSequence, asLast)
  }

  def handleEvents(nextSequence: Long, availableSequence: Long): Option[Long]  = {
    for (index <- nextSequence to availableSequence){
      handle(nextSequence, nextSequence == availableSequence)
    }
    Some(availableSequence)

  }

  def loop() {

    def processEvents(fromIndex: Long) = {
      for {
        availableSequence <- nextSequence(fromIndex);
        nextIndex <- handleEvents(fromIndex, availableSequence)
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