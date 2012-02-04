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
    "get back expected number of events except one"                 ! e2

  implicit val config = Configuration(ringBufferSize = 16, iterations = 64, runs = 1)

  def setupFor(configuration: Configuration, handler: LifeCycleAware[ValueEvent]) = {
    val rb = ringBuffer(ValueEventFactory, size = configuration.ringBufferSize);

    val processor = BatchEventProcessor.withLifeCycle[ValueEvent](rb, rb.newBarrier(), handler);
    rb.setGatingSequences(processor.getSequence)

    val shooter = Shooter(config.iterations, rb, EventModuleStub.fillEvent)
    (processor, shooter)
  }

  def run(processor: MonitoredBatchEventProcessor[EventModuleStub.ValueEvent] , shooter: Actor, handler: EventHandlerLifeCycleAware[ValueEvent]) {
    playWith(List(processor)) {
      shooter ! 'fire
      handler.latch.await(5, SECONDS)
    }
  }

  def e1(implicit configuration: Configuration) = {
    val handler = EventHandlerLifeCycleAware[ValueEvent](new CountDownLatch(1), configuration.iterations)

    val (processor, shooter) = setupFor(configuration, handler)
    run(processor, shooter, handler)

    handler.wasStarted.should(beEqualTo(true))
      .and(handler.wasStopped.should(beEqualTo(true)))
        .and(handler.count should (beEqualTo(configuration.iterations)))

  }

  def e2(implicit configuration: Configuration) = {
    val handler = EventHandlerLifeCycleAware[ValueEvent](new CountDownLatch(1), configuration.iterations, fail = true)

    val (processor, shooter) = setupFor(configuration, handler)
    run(processor, shooter, handler)

    handler.wasStarted.should(beEqualTo(true))
      .and(handler.wasStopped.should(beEqualTo(true)))
        .and(handler.count should (beEqualTo(handler.failureIndex)))

  }


  override def challenge(implicit configuration: Configuration) = 0L
}