package io.vertx.ext.reactivestreams.akka.test

import java.util.concurrent.{CountDownLatch, TimeUnit}

import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.ext.reactivestreams.ReactiveReadStream
import junit.framework.Assert._
import org.junit.Test
import org.reactivestreams.{Subscription, Subscriber}

/**
 * Created by Jochen Mader
 */
class AkkaRandomValuesStreamTest {

  @Test
  def testWithReactiveReadStream {
    val rws: ReactiveReadStream[Buffer] = ReactiveReadStream.readStream[Buffer]
    val cl = new CountDownLatch(1)
    rws.handler(new Handler[Buffer] {
      override def handle(e: Buffer): Unit = cl.countDown()
    })
    val akkaRandomValuesStream = new AkkaRandomValuesStream(rws)
    rws.resume
    assertTrue("Timed out while waiting for events", cl.await(200, TimeUnit.MILLISECONDS))
  }

  @Test
  def testWithCustomSubscriber {
    val cl = new CountDownLatch(1)

    val akkaRandomValuesStream = new AkkaRandomValuesStream(new Subscriber[Buffer] {override def onError(throwable: Throwable): Unit = ???

      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)

      override def onComplete(): Unit = ???

      override def onNext(t: Buffer): Unit = cl.countDown()

    });

    assertTrue("Timed out while waiting for events", cl.await(200, TimeUnit.MILLISECONDS))
  }
}
