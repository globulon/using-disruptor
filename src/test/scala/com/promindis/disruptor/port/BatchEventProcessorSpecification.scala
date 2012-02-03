package com.promindis.disruptor.port

import org.specs2.Specification
import com.promindis.disruptor.adapters.RingBufferFactory._
import com.promindis.disruptor.port.EventModuleStub._
import com.promindis.disruptor.adapters.Shooter
import com.promindis.disruptor.configurations.{Scenario, Configuration}

import java.util.concurrent.{TimeUnit, CountDownLatch}
import TimeUnit._

final class BatchEventProcessorSpecification extends Specification with Scenario{ def is =
  "Specification to check the 'BatchEventProcessor' behavior"           ^
                                                                        p^
    "In nominal scenario the BatchEventProcessor test handler should"   ^
    "get back the expected number of event"                             ! e1

  implicit val config = Configuration(ringBufferSize = 16, iterations = 64, runs = 1)

  def e1(implicit configuration: Configuration) = {
    val rb = ringBuffer(ValueEventFactory,size = configuration.ringBufferSize);

    val barrier =  rb.newBarrier()
    val countDownLatch = new CountDownLatch(1)
    val handler = EventHandlerLifeCycleAware[ValueEvent](countDownLatch, configuration.iterations)
    val processor = BatchEventProcessor.withLifeCycle[ValueEvent](rb, barrier, handler);
    rb.setGatingSequences(processor.getSequence)

    val shooter = Shooter(config.iterations, rb, EventModuleStub.fillEvent)

    playWith(List(processor)){
      shooter ! 'fire
      countDownLatch.await(5, SECONDS)
    }

    handler.wasStarted.should(beEqualTo(true))
      .and(handler.wasStopped.should(beEqualTo(true)))
        .and(handler.count should (beEqualTo(configuration.iterations)))

  }

  override def challenge(implicit configuration: Configuration) = 0L
}