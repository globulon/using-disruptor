package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicBoolean
import com.lmax.disruptor.{AlertException , Sequence, Sequencer, EventProcessor, RingBuffer, SequenceBarrier}
import Sequencer._
import com.promindis.disruptor.adapters.Processor
import annotation.tailrec
import com.promindis.disruptor.support.Utils
import Utils._

trait BatchEventProcessor[T] extends Processor with EventProcessor {
  type Handler <: EventHandler[T]
  val ringBuffer: RingBuffer[T]
  val  sequenceBarrier: SequenceBarrier
  val eventHandler: Handler
  val exceptionHandler: ExceptionHandler

  val running: AtomicBoolean = new AtomicBoolean(false)
  val sequence: Sequence = new Sequence(INITIAL_CURSOR_VALUE)

  override def halt() {
    stopRunning()
    sequenceBarrier.alert()
  }

  private def alert: PartialFunction[Throwable, Option[Long]] = {
    case ex: AlertException if (!running.get())=> None
  }

  private def trap[E](sequence: Long, forEvent: E): PartialFunction[Throwable, Option[Long]] = {
    case ex: Throwable =>
      exceptionHandler.handleEventException(ex, sequence, forEvent)
  }

  private def nextSequence(l: Long) = {
    managing (alert){
      sequenceBarrier.waitFor(l)
    }
  }

  private def handlingEvent(sequence: Long, asLast: Boolean): Option[Long] = {
    val event = ringBuffer.get(sequence)
    managing(alert orElse trap(sequence, event)) {
      eventHandler.onEvent(event, sequence, asLast)
      sequence
    }
  }

  @tailrec private def handleEvents(index: Long, availableSequence: Long): Option[Long]  = {
    handlingEvent(index, index == availableSequence ) match {
      case Some(step) if step == availableSequence => Some(step)
      case Some(step) => handleEvents(step + 1L, availableSequence)
      case _ => {
        println("resend none for sequence: " + index)
        None
      }
    }
  }

  def processEvents(fromIndex: Long) = {
    for {
      availableSequence <- nextSequence(fromIndex);
      nextIndex <- handleEvents(fromIndex, availableSequence)
    } yield nextIndex
  }

  private def loop() {
    @tailrec def innerLoop(fromIndex: Long) {
      val lastIndex = processEvents(fromIndex)
      if (lastIndex.isDefined)  {
        sequence.set(lastIndex.get)
        innerLoop(lastIndex.get + 1)
      }
    }

    innerLoop(sequence.get + 1L)
  }

  protected def stopRunning() {
    running.set(false)
  }

  protected def startRunning() {
    sequenceBarrier.clearAlert()
  }

  override def run() {
    if (running.compareAndSet(false, true)) {
      startRunning()
      loop()
      stopRunning()
    }
  }

  override def getSequence = sequence
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
  def apply[T](rb: RingBuffer[T], sb: SequenceBarrier, handler: EventHandler[T], exHandler: ExceptionHandler = FatalExceptionHandler()) =
    new BatchEventProcessor[T]{
      type Handler = EventHandler[T]
      val eventHandler = handler
      val ringBuffer = rb
      val sequenceBarrier = sb
      val exceptionHandler = exHandler
    }

  def withLifeCycle[T](rb: RingBuffer[T], sb: SequenceBarrier, handler: LifeCycleAware[T], exHandler: ExceptionHandler = FatalExceptionHandler()) =
    new MonitoredBatchEventProcessor[T]{
      type Handler = LifeCycleAware[T]
      val eventHandler = handler
      val ringBuffer = rb
      val sequenceBarrier = sb
      val exceptionHandler = exHandler
    }

}