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
  val exceptionHandler: ExceptionHandler

  val running: AtomicBoolean = new AtomicBoolean(false)
  val sequence: Sequence = new Sequence(INITIAL_CURSOR_VALUE)

  override def halt() {
    stopRunning()
    sequenceBarrier.alert()
  }

  @inline
  private def nextSequence(l: Long) = {
    try {
      Some(sequenceBarrier.waitFor(l))
    } catch  {
      case ex: AlertException if (!running.get())=> None
      case ex: InterruptedException => None
    }
  }

  @inline
  private def handlingEvent(sequence: Long, asLast: Boolean): Option[Long] = {
    val event = ringBuffer.get(sequence)
    try {
      eventHandler.onEvent(event, sequence, asLast)
    } catch {
      case ex: Throwable =>
        println("Caught exception...")
        exceptionHandler.handleEventException(ex, sequence, event)
    }
  }

  @tailrec private def handleEvents(index: Long, availableSequence: Long): Option[Long]  = {
    val last = index == availableSequence
    handlingEvent(index, last) match {
      case lastIndex if last  => lastIndex
      case Some(_) => handleEvents(index + 1L, availableSequence)
      case _ => None
    }
  }

  @inline
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