package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicBoolean
import com.lmax.disruptor.{AlertException , Sequence, Sequencer, EventProcessor, RingBuffer, SequenceBarrier}
import Sequencer._
import com.promindis.disruptor.adapters.Processor
import annotation.tailrec

trait BatchEventProcessor[T] extends Processor with EventProcessor {
  type Handler <: EventHandler[T]
  val ringBuffer: RingBuffer[T]
  val  sequenceBarrier: SequenceBarrier
  val eventHandler: Handler

  val running: AtomicBoolean = new AtomicBoolean(false)
  val exceptionHandler: ExceptionHandler = FatalExceptionHandler()
  val sequence: Sequence = new Sequence(INITIAL_CURSOR_VALUE)

  override def halt() {
    stopRunning()
    sequenceBarrier.alert()
  }

  def alert: PartialFunction[Throwable, Option[Long]] = {
    case ex: AlertException if (!running.get())=> None
  }

  def trap[E](sequence: Long, forEvent: E): PartialFunction[Throwable, Option[Long]] = {
    case ex: Throwable =>
      exceptionHandler.handleEventException(ex, sequence, forEvent)
      None
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
    val lastSequence: Option[Long] = (nextSequence to availableSequence).view.dropWhile{ index =>
      handlingEvent(index, index == availableSequence).isDefined
    }.take(1).headOption

    if (lastSequence.toLeft(availableSequence).isLeft) None else Some(availableSequence)
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

  def startRunning() {
    sequenceBarrier.clearAlert()
  }

  override def run() {
    if (running.compareAndSet(false, true)) {
      startRunning()
      loop()
      stopRunning()
    }
  }

  def getSequence = sequence
}

trait MonitoredBatchEventProcessor[T] extends BatchEventProcessor[T] {
  type Handler <: LifeCycleAware[T]

  override def startRunning() {
    super.startRunning()
    eventHandler.started()
  }

  override def stopRunning()  {
    super.stopRunning()
    eventHandler.stopped()
  }
}

object BatchEventProcessor {
  def apply[T](rb: RingBuffer[T], sb: SequenceBarrier, handler: EventHandler[T]) =
    new BatchEventProcessor[T]{
      type Handler = EventHandler[T]
      val eventHandler = handler
      val ringBuffer = rb
      val sequenceBarrier = sb
    }

  def withLifeCycle[T](rb: RingBuffer[T], sb: SequenceBarrier, handler: LifeCycleAware[T]) =
    new MonitoredBatchEventProcessor[T]{
      type Handler = LifeCycleAware[T]
      val eventHandler = handler
      val ringBuffer = rb
      val sequenceBarrier = sb
    }

}