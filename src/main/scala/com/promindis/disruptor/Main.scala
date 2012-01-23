package com.promindis.disruptor

import collection.immutable.IndexedSeq
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor._
import java.util.concurrent.{CountDownLatch, Executors}

case class MessageEvent(var message: Any)

case class MessageEventTranslator(message: Any) extends EventTranslator[MessageEvent] {
  def translateTo(event: MessageEvent, sequence: Long) = {
    event.message = message
    event
  }
}

trait DisruptorActor extends EventHandler[MessageEvent] {

  val ring_size = 1024 * 8
  val factory = new EventFactory[MessageEvent] {
    def newInstance() = MessageEvent(null)
  }
  val disruptor = new Disruptor[MessageEvent](factory, DisruptorActor.executor, new SingleThreadedClaimStrategy(ring_size), new SleepingWaitStrategy())

  def receive: PartialFunction[Any, Unit]

  override def onEvent(event: MessageEvent, sequence: Long, endOfBatchSequence: Boolean) {
    receive(event.message)
  }

  def start() {
    disruptor.handleEventsWith(this)
    disruptor.start()
  }

  def stop() {
    disruptor.shutdown()
  }

  def !(message: Any) {
    disruptor.publishEvent(MessageEventTranslator(message))
  }
}

object DisruptorActor {
  val executor = Executors.newFixedThreadPool(100)
}

class Producer(actors: Seq[MyDisruptorActor], numMsgsPerActor: Int) extends Runnable {

  override def run() {
    for {
      actor <- actors
      i <- 1 to numMsgsPerActor
    } {
      actor ! Ping
    }
  }
}

case class Ping()

class MyDisruptorActor(id: String, numMsgs: Int) extends DisruptorActor {

  val latch = new CountDownLatch(numMsgs)

  def receive = {
    case Ping => {
      latch.countDown()
      //if (latch.getCount % 100000 == 0) {
      //  println(id + " .")
      //}
    }
  }
}

object Main {
  def main(args: Array[String]) {

    val numActors = 100
    val numMsgsPerActor = 1000000
    var delta: Long = 0

    for (m <- 1 to 5) {

      val actors: IndexedSeq[MyDisruptorActor] = for {
        i <- 1 to numActors
        actor = {
          val a = new MyDisruptorActor(i.toString, numMsgsPerActor)
          a.start()
          a
        }
      } yield actor

      val start = System.currentTimeMillis()

      new Thread(new Producer(actors, numMsgsPerActor)).start()

      for (actor <- actors) {
        actor.latch.await()
        actor.stop()
      }

      val stop = System.currentTimeMillis()
      delta += stop - start
      println(m)

    }
    DisruptorActor.executor.shutdown()
    println((numActors * numMsgsPerActor) * 5000 / delta)
  }


}