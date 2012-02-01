package com.promindis.disruptor.port

import java.util.concurrent.atomic.AtomicBoolean
import com.lmax.disruptor._
import annotation.tailrec
import com.promindis.disruptor.adapters.Processor
import com.promindis.disruptor.support.Utils

/**
 * Convenience class for handling the batching semantics of consuming entries from a {@link RingBuffer}
 * and delegating the available events to a {@link EventHandler}.
 *
 * If the {@link EventHandler} also implements {@link LifecycleAware} it will be notified just after the thread
 * is started and just before the thread is shutdown.
 *
 * T event implementation storing the data for sharing during exchange or parallel coordination of an event.
 */
final case class BatchEventProcessor[T](ringBuffer: RingBuffer[T], sequenceBarrier: SequenceBarrier, eventHandler: EventHandler[T])
  extends Processor with EventProcessor {
  val running: AtomicBoolean = new AtomicBoolean(false)
  var exceptionHandler: ExceptionHandler = new FatalExceptionHandler()
  val sequence: Sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE)

  if (eventHandler.isInstanceOf[SequenceReportingEventHandler[_]]) {
    (eventHandler.asInstanceOf[SequenceReportingEventHandler[_]]).setSequenceCallback(sequence)
  }

  override def halt() {
    stopRunning()
    sequenceBarrier.alert()
  }

  /**
   * Set a new {@link ExceptionHandler} for handling exceptions propagated out of the {@link BatchEventProcessor}
   *
   * @param exceptionHandler to replace the existing exceptionHandler.
   */
  def setExceptionHandler(exceptionHandler: ExceptionHandler) {
    if (null == exceptionHandler) {
      throw new NullPointerException
    }
    this.exceptionHandler = exceptionHandler
  }

  /**
   * It is ok to have another thread rerun this method after a halt().
   */


  def loop() {
    @tailrec def handleSequences(from: Long, to: Long): Option[Long] = {
      if (from > to) {
        Some(from)
      } else {
        eventHandler.onEvent(ringBuffer.get(from), from, from == to)
        handleSequences(from + 1, to)
      }
    }

    def availableFrom(nextSequence: Long): Option[Long] = {
      try {
        Some(sequenceBarrier.waitFor(nextSequence))
      } catch {
        case _ : AlertException if (!running.get())=> None
        case _ : AlertException => Some(nextSequence)
        case _ : Throwable =>
          ////          exceptionHandler.handleEventException(ex, nextSequence, event)
          Some(nextSequence)
      }
    }

    @tailrec def innerLoop(nextSequence: Long) {
      val newNextSequence = for {
        availableSeq <- availableFrom(nextSequence);
        value <- handleSequences(nextSequence, availableSeq)
      } yield value

      newNextSequence match {
        case Some(value) =>
          sequence.set(value - 1L)
          innerLoop(value)
        case _ =>
      }
    }

    innerLoop(sequence.get + 1L)
  }

  def stopRunning() {
    running.set(false)
  }

  override def run() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("Thread is already running")
    }
    sequenceBarrier.clearAlert()
    notifyStart()
    loop()
    notifyShutdown()
    stopRunning()
  }

  private def notifyStart() {
    if (eventHandler.isInstanceOf[LifecycleAware]) {
      try {
        (eventHandler.asInstanceOf[LifecycleAware]).onStart()
      }
      catch {
        case ex: Throwable => {
          exceptionHandler.handleOnStartException(ex)
        }
      }
    }
  }

  private def notifyShutdown() {
    if (eventHandler.isInstanceOf[LifecycleAware]) {
      try {
        (eventHandler.asInstanceOf[LifecycleAware]).onShutdown()
      }
      catch {
        case ex: Throwable => {
          exceptionHandler.handleOnShutdownException(ex)
        }
      }
    }
  }

  def getSequence = sequence
}

object BatcEventProcessor {
  def apply[T](ringBuffer: RingBuffer[T], sequenceBarrier: SequenceBarrier, eventHandler: EventHandler[T]) =
    new BatchEventProcessor[T](ringBuffer, sequenceBarrier, eventHandler)
}