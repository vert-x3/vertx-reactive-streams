/*
 * Copyright 2014 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.reactivestreams.test;

import io.vertx.core.VertxException;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveReadStreamTest extends ReactiveStreamTestBase {


  @Test
  public void testSubscribe() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    assertNotNull(publisher.subscription);
    assertEquals(1, publisher.subscription.requestedTimes);
  }

  @Test
  public void testDatahandler() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    assertNotNull(publisher.subscription);
    assertEquals(1, publisher.subscription.requestedTimes);
    List<Buffer> received = new ArrayList<>();
    rws.handler(received::add);
    assertEquals(1, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE, publisher.subscription.requested);
    int numBuffers = 4;
    List<Buffer> buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    assertEquals(numBuffers, received.size());
    for (int i = 0; i < numBuffers; i++) {
      assertEquals(buffers.get(i), received.get(i));
    }
    assertEquals(2, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE * 2, publisher.subscription.requested);
  }

  @Test
  public void testSetPausedDataHandler() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    rws.pause();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    assertNotNull(publisher.subscription);
    assertEquals(0, publisher.subscription.requestedTimes);
    List<Buffer> received = new ArrayList<>();
    rws.handler(received::add);
    assertEquals(0, publisher.subscription.requestedTimes);
    assertEquals(0, publisher.subscription.requested);

    assertEquals(0, received.size());
    assertEquals(0, publisher.subscription.requestedTimes);
    assertEquals(0, publisher.subscription.requested);
    rws.resume();
    assertEquals(1, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE, publisher.subscription.requested);
    int numBuffers = 4;
    List<Buffer> buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    assertEquals(numBuffers, received.size());
    for (int i = 0; i < numBuffers; i++) {
      assertEquals(buffers.get(i), received.get(i));
    }
  }

  @Test
  public void testPauseInHandler() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    assertNotNull(publisher.subscription);
    assertEquals(1, publisher.subscription.requestedTimes);
    List<Buffer> received = new ArrayList<>();
    rws.handler(buff -> {
      received.add(buff);
      rws.pause();
    });
    assertEquals(1, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE, publisher.subscription.requested);
    int numBuffers = 4;
    List<Buffer> buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    assertEquals(1, received.size());
    assertEquals(buffers.get(0), received.get(0));

    assertEquals(1, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE, publisher.subscription.requested);
  }

  @Test
  public void testPauseResume() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    assertNotNull(publisher.subscription);
    assertEquals(1, publisher.subscription.requestedTimes);
    List<Buffer> received = new ArrayList<>();
    rws.handler(received::add);
    assertEquals(1, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE, publisher.subscription.requested);
    int numBuffers = 4;
    List<Buffer> buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    assertEquals(numBuffers, received.size());
    for (int i = 0; i < numBuffers; i++) {
      assertEquals(buffers.get(i), received.get(i));
    }
    rws.pause();
    assertEquals(2, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE * 2, publisher.subscription.requested);
    rws.resume();
    assertEquals(2, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE * 2, publisher.subscription.requested);
    buffers.clear();
    received.clear();
    buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    assertEquals(numBuffers, received.size());
    for (int i = 0; i < numBuffers; i++) {
      assertEquals(buffers.get(i), received.get(i));
    }
  }

  @Test
  public void testPauseResumeInHandler() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    assertNotNull(publisher.subscription);
    assertEquals(1, publisher.subscription.requestedTimes);
    List<Buffer> received = new ArrayList<>();
    rws.handler(buff -> {
      rws.pause();
      rws.resume();
      received.add(buff);
    });
    assertEquals(1, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE, publisher.subscription.requested);
    int numBuffers = 4;
    List<Buffer> buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    assertEquals(numBuffers, received.size());
    for (int i = 0; i < numBuffers; i++) {
      assertEquals(buffers.get(i), received.get(i));
    }
    assertEquals(2, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE * 2, publisher.subscription.requested);
  }

  @Test
  public void testFetch() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    assertNotNull(publisher.subscription);
    assertEquals(1, publisher.subscription.requestedTimes);
    List<Buffer> received = new ArrayList<>();
    rws.handler(received::add);
    assertEquals(1, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE, publisher.subscription.requested);
    int numBuffers = 4;
    List<Buffer> buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    assertEquals(numBuffers, received.size());
    for (int i = 0; i < numBuffers; i++) {
      assertEquals(buffers.get(i), received.get(i));
    }
    rws.pause();
    assertEquals(2, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE * 2, publisher.subscription.requested);
    assertEquals(2, publisher.subscription.requestedTimes);
    assertEquals(ReactiveReadStream.DEFAULT_BATCH_SIZE * 2, publisher.subscription.requested);
    buffers.clear();
    received.clear();
    buffers = createRandomBuffers(numBuffers);
    for (Buffer buffer: buffers) {
      publisher.subscriber.onNext(buffer);
    }
    for (int i = 0;i < numBuffers;i++) {
      rws.fetch(1);
      assertEquals(1 + i, received.size());
    }
    for (int i = 0; i < numBuffers; i++) {
      assertEquals(buffers.get(i), received.get(i));
    }
  }

  @Test
  public void testOnError() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    rws.exceptionHandler(t -> {
      assertTrue(t instanceof VertxException);
      assertEquals("foo", t.getMessage());
      testComplete();
    });
    publisher.subscriber.onError(new VertxException("foo"));
    await();
  }

  @Test
  public void testOnComplete() throws Exception {
    ReactiveReadStream<Buffer> rws = ReactiveReadStream.readStream();
    MyPublisher publisher = new MyPublisher();
    publisher.subscribe(rws);
    rws.endHandler(v -> {
      testComplete();
    });
    publisher.subscriber.onComplete();
    await();
  }

  class MySubscription implements Subscription {

    int requested;
    int requestedTimes;

    @Override
    public void request(long n) {
      requestedTimes++;
      requested += n;
    }

    @Override
    public void cancel() {

    }
  }


  class MyPublisher implements Publisher<Buffer> {

    MySubscription subscription;
    Subscriber<? super Buffer> subscriber;

    @Override
    public void subscribe(Subscriber<? super Buffer> subscriber) {
      this.subscriber = subscriber;
      subscription = new MySubscription();
      subscriber.onSubscribe(subscription);
    }
  }

}
