package io.vertx.ext.reactivestreams.akka.test

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{Broadcast, FlowGraph, Sink, Source}
import io.vertx.core.buffer.Buffer
import org.reactivestreams.Subscriber

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class AkkaRandomValuesStream(subscriber: Subscriber[Buffer]) {
  implicit val system = ActorSystem("Sys")
  implicit val materializer = ActorFlowMaterializer()
  val bufferSource: Source[Buffer, Unit] =
    Source(() => Iterator.continually(Buffer.buffer("random value")))
  val mySink = Sink.apply(subscriber);
  val consoleSink = Sink.foreach[Buffer] { value =>
//      println("console "+value.getString(0, value.length()))
    ()
  }

  val materialized = FlowGraph.closed(consoleSink, mySink)((console, _) => console) { implicit builder =>
    (console, mine) =>
      import FlowGraph.Implicits._
      val broadcast = builder.add(Broadcast[Buffer](2))
      bufferSource ~> broadcast ~> console
      broadcast ~> mine
  }.run()

  materialized.onComplete {
    case Success(_) =>
      system.shutdown()
    case Failure(e) =>
      println(s"Failure: ${e.getMessage}")
      system.shutdown()
  }
}
