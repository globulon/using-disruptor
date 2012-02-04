package com.promindis.disruptor.port

import org.specs2.Specification
import com.promindis.disruptor.adapters.RingBufferFactory._
import com.promindis.disruptor.port.EventModuleStub._
import com.promindis.disruptor.adapters.Shooter
import com.promindis.disruptor.configurations.{Scenario, Configuration}
import scala.actors.Actor
import java.util.concurrent.{TimeUnit, CountDownLatch}
import TimeUnit._

final class BatchEventProcessorSpecification extends Specification with Scenario{ def is =
  "Specification to check the 'BatchEventProcessor' behavior"           ^
                                                                        p^
    "In nominal scenario the BatchEventProcessor test handler should"   ^
    "get back expected number of event"                             ! e1^
    p^
    "In failing scenario the BatchEventProcessor test handler should"   ^
    "get back expected number of events except one"                 ! e2


  implicit val config = Configuration(ringBufferSize = 16, iterations = 64, runs = 1)

  def setupFor(handler: LifeCycleAware[ValueEvent])(implicit configuration: Configuration) = {
    val rb = ringBuffer(ValueEventFactory, size = configuration.ringBufferSize);
    val processor = BatchEventProcessor.withLifeCycle[ValueEvent](rb, rb.newBarrier(), handler, SilentExceptionHandler());
    rb.setGatingSequences(processor.getSequence)

    val shooter = Shooter(configuration.iterations, rb, EventModuleStub.fillEvent)
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
    val handler = EventHandlerLifeCycleAware[ValueEvent](new CountDownLatch(1), configuration.iterations)
    run(setupFor(handler), handler)
    import handler._
    import configuration._

    handler.wasStarted
      .and(handler.wasStopped)
        .and(handledEvents should (beEqualTo(iterations)))
          .and(receivedEvents should (beEqualTo(iterations)))
  }

  def e2(implicit configuration: Configuration) = {
    val handler = EventHandlerLifeCycleAware[ValueEvent](new CountDownLatch(1), configuration.iterations, fail = true)
    import handler._
    run(setupFor(handler), handler)

    handler.wasStarted
      .and(handler.wasStopped)
        .and(handledEvents should (beEqualTo(failureIndex)))
          .and(receivedEvents should (beEqualTo(handledEvents + 1L)))
 }


  override def challenge(implicit configuration: Configuration) = 0L
}