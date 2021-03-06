package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicBoolean
import com.promindis.disruptor.adapters.Processor
import annotation.tailrec

trait BatchEventProcessor[T] extends Processor {
  type Handler <: EventHandler[T]
  def ringBuffer: RingBuffer[T]
  def  sequenceBarrier: SequencesBarrier
  def eventHandler: Handler
  def exceptionHandler: ExceptionHandler

  val running: AtomicBoolean = new AtomicBoolean(false)
  val sequence = RSequence()

  override def halt() {
    stopRunning()
    sequenceBarrier.doAlert()
  }

  @inline
  private def nextSequence(l: Long) = {
    try {
      sequenceBarrier.waitFor(l)
    } catch  {
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
        exceptionHandler.handleEventException(ex, sequence, event)
    }
  }

  @tailrec private def handleEvents(index: Long, availableSequence: Long): Option[Long]  = {
    val last = index >= availableSequence
    handlingEvent(index, last) match {
      case lastIndex if last  => lastIndex
      case None => None
      case _ => handleEvents(index + 1L, availableSequence)
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
  def apply[T](rb: RingBuffer[T], sb: SequencesBarrier, handler: EventHandler[T], exHandler: ExceptionHandler = FatalExceptionHandler()) =
    new BatchEventProcessor[T]{
      type Handler = EventHandler[T]
      override lazy val eventHandler = handler
      override val ringBuffer = rb
      override val sequenceBarrier = sb
      override val exceptionHandler = exHandler
    }

  def withLifeCycle[T](rb: RingBuffer[T], sb: SequencesBarrier, handler: LifeCycleAware[T], exHandler: ExceptionHandler = FatalExceptionHandler()) =
    new MonitoredBatchEventProcessor[T]{
      type Handler = LifeCycleAware[T]
      override lazy val eventHandler = handler
      override lazy val ringBuffer = rb
      override lazy val sequenceBarrier = sb
      override lazy val exceptionHandler = exHandler
    }

}