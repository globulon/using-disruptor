package com.promindis.disruptor.port

import org.specs2.Specification
import com.promindis.disruptor.adapters.RingBufferFactory._
import EventModuleStub._
import com.promindis.disruptor.configurations.{Scenario, Configuration}
import scala.actors.Actor
import java.util.concurrent.{TimeUnit, CountDownLatch}
import TimeUnit._
import com.promindis.disruptor.adapters.{ProcessorFactory, Shooter}

class BatchEventProcessorSpecification extends Specification with Scenario{ def is =
  "Specification to check the 'BatchEventProcessor' behavior"           ^
                                                                        p^
    "In nominal scenario the BatchEventProcessor test handler should"   ^
    "get back expected number of event"                             ! e1^
    p^
    "In fatal failing scenario the BatchEventProcessor should"   ^
    "process only the successfuly handled event and one unsuccessful"! e2 ^
    p^
  "In tolerant failing scenario the BatchEventProcessor should"   ^
    "process all the  events"    ! e3



  implicit val config = Configuration(ringBufferSize = 16, iterations = 64, runs = 1)

  def setupFor(handler: LifeCycleAware[ValueEvent], exceptionHandler: ExceptionHandler)(implicit configuration: Configuration) = {
    val Some(rb) = ringBuffer(ValueEventFactory, size = configuration.ringBufferSize.toInt);
    val processor = BatchEventProcessor.withLifeCycle(rb, rb.barrier, handler, exceptionHandler);
    rb.withGating(processor.getSequence)

    val shooter = Shooter(configuration.iterations, rb, fillEvent)
    (processor, shooter)
  }

  def run[T](components: (BatchEventProcessor[T] , Actor), handler: EventHandlerLifeCycleAware[T]) {
    val (processor, shooter) = components
    playWith(List(processor)) {
      shooter ! 'fire
      handler.latch.await(5, SECONDS)
    }
  }

  def e1(implicit configuration: Configuration) = {
    val eventHandler = EventHandlerLifeCycleAware[ValueEvent](new CountDownLatch(1), configuration.iterations)
    run(setupFor(eventHandler, SilentExceptionHandler.fatal()), eventHandler)
    import eventHandler._
    import configuration._

    eventHandler.wasStarted
      .and(eventHandler.wasStopped)
        .and(handledEvents should (beEqualTo(iterations)))
          .and(receivedEvents should (beEqualTo(iterations)))
  }

  def e2(implicit configuration: Configuration) = {
    val eventHandler = EventHandlerLifeCycleAware[ValueEvent](new CountDownLatch(1), configuration.iterations, fail = true)
    import eventHandler._
    run(setupFor(eventHandler, SilentExceptionHandler.fatal()), eventHandler)

    eventHandler.wasStarted
      .and(eventHandler.wasStopped)
        .and(handledEvents should (beEqualTo(failureIndex)))
          .and(receivedEvents should (beEqualTo(handledEvents + 1L)))
}

  def e3(implicit configuration: Configuration) = {
    val eventHandler = EventHandlerLifeCycleAware[ValueEvent](new CountDownLatch(1), configuration.iterations, fail = true)
    import eventHandler._
    import configuration._
    run(setupFor(eventHandler, SilentExceptionHandler.tolerant()), eventHandler)

    eventHandler.wasStarted
      .and(eventHandler.wasStopped)
      .and(handledEvents should (beEqualTo(iterations - 1)))
      .and(receivedEvents should (beEqualTo(iterations)))
  }

  def challenge(implicit configuration: Configuration, factory: ProcessorFactory) = 0
}