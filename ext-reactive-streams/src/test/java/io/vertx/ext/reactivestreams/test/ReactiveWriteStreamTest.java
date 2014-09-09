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

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import io.vertx.test.core.TestUtils;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveWriteStreamTest extends ReactiveStreamTestBase {

  // TODO tests with fake subscriber

  @Test
  public void testWriteNoTokensInitially() throws Exception {
    ReactiveWriteStream rws = ReactiveWriteStream.writeStream();

    MySubscriber subscriber = new MySubscriber();
    rws.subscribe(subscriber);

    List<Buffer> buffers = createRandomBuffers(4);
    for (Buffer buffer: buffers) {
      rws.write(buffer);
    }

    assertTrue(subscriber.buffers.isEmpty());

    subscriber.subscription.request(1);
    assertEquals(1, subscriber.buffers.size());
    assertSame(buffers.get(0), subscriber.buffers.get(0));

    subscriber.subscription.request(2);
    assertEquals(3, subscriber.buffers.size());
    assertSame(buffers.get(1), subscriber.buffers.get(1));
    assertSame(buffers.get(2), subscriber.buffers.get(2));

  }

  @Test
  public void testWriteInitialTokens() throws Exception {
    ReactiveWriteStream rws = ReactiveWriteStream.writeStream();

    MySubscriber subscriber = new MySubscriber();
    rws.subscribe(subscriber);

    subscriber.subscription.request(3);

    List<Buffer> buffers = createRandomBuffers(4);
    for (Buffer buffer: buffers) {
      rws.write(buffer);
    }

    assertEquals(3, subscriber.buffers.size());
    assertSame(buffers.get(0), subscriber.buffers.get(0));
    assertSame(buffers.get(1), subscriber.buffers.get(1));
    assertSame(buffers.get(2), subscriber.buffers.get(2));

  }

  @Test
  public void testMaxBufferSize() throws Exception {
    int size = 54651;
    testDefaultMaxBufferSize(ReactiveWriteStream.writeStream().setBufferMaxSize(size), size);
  }

  @Test
  public void testDefaultMaxBufferSize() throws Exception {
    testDefaultMaxBufferSize(ReactiveWriteStream.writeStream(), ReactiveWriteStream.DEFAULT_MAX_BUFFER_SIZE);
  }

  // TODO test setters for max writestreamsize and buffer size and valid values

  // TODO test cancel subscription

  private void testDefaultMaxBufferSize(ReactiveWriteStream rws, int maxSize) throws Exception {

    MySubscriber subscriber = new MySubscriber();
    rws.subscribe(subscriber);

    subscriber.subscription.request(2);

    Buffer buff = TestUtils.randomBuffer(maxSize + 100);
    rws.write(buff);

    assertEquals(2, subscriber.buffers.size());
    assertEquals(buff.slice(0, maxSize), subscriber.buffers.get(0));
    assertEquals(buff.slice(maxSize, maxSize + 100), subscriber.buffers.get(1));

  }

  @Test
  public void testMultipleSubscribers() throws Exception {
    ReactiveWriteStream rws = ReactiveWriteStream.writeStream();

    MySubscriber subscriber1 = new MySubscriber();
    rws.subscribe(subscriber1);
    MySubscriber subscriber2 = new MySubscriber();
    rws.subscribe(subscriber2);
    MySubscriber subscriber3 = new MySubscriber();
    rws.subscribe(subscriber3);

    List<Buffer> buffers = createRandomBuffers(10);
    for (Buffer buffer: buffers) {
      rws.write(buffer);
    }

    assertEquals(0, subscriber1.buffers.size());
    assertEquals(0, subscriber2.buffers.size());
    assertEquals(0, subscriber3.buffers.size());

    // We go at the speed of the slowest consumer
    subscriber1.subscription.request(1);
    assertEquals(0, subscriber1.buffers.size());
    assertEquals(0, subscriber2.buffers.size());
    assertEquals(0, subscriber3.buffers.size());

    subscriber2.subscription.request(1);
    assertEquals(0, subscriber1.buffers.size());
    assertEquals(0, subscriber2.buffers.size());
    assertEquals(0, subscriber3.buffers.size());

    subscriber3.subscription.request(1);
    assertEquals(1, subscriber1.buffers.size());
    assertEquals(1, subscriber2.buffers.size());
    assertEquals(1, subscriber3.buffers.size());
    assertEquals(buffers.get(0), subscriber1.buffers.get(0));
    assertEquals(buffers.get(0), subscriber2.buffers.get(0));
    assertEquals(buffers.get(0), subscriber3.buffers.get(0));

    subscriber1.subscription.request(4);
    assertEquals(1, subscriber1.buffers.size());
    assertEquals(1, subscriber2.buffers.size());
    assertEquals(1, subscriber3.buffers.size());
    subscriber2.subscription.request(3);
    assertEquals(1, subscriber1.buffers.size());
    assertEquals(1, subscriber2.buffers.size());
    assertEquals(1, subscriber3.buffers.size());
    subscriber3.subscription.request(2);
    assertEquals(3, subscriber1.buffers.size());
    assertEquals(3, subscriber2.buffers.size());
    assertEquals(3, subscriber3.buffers.size());
    assertEquals(buffers.get(0), subscriber1.buffers.get(0));
    assertEquals(buffers.get(1), subscriber1.buffers.get(1));
    assertEquals(buffers.get(2), subscriber1.buffers.get(2));
    assertEquals(buffers.get(0), subscriber2.buffers.get(0));
    assertEquals(buffers.get(1), subscriber2.buffers.get(1));
    assertEquals(buffers.get(2), subscriber2.buffers.get(2));
    assertEquals(buffers.get(0), subscriber3.buffers.get(0));
    assertEquals(buffers.get(1), subscriber3.buffers.get(1));
    assertEquals(buffers.get(2), subscriber3.buffers.get(2));

    subscriber2.subscription.request(1);
    assertEquals(3, subscriber1.buffers.size());
    assertEquals(3, subscriber2.buffers.size());
    assertEquals(3, subscriber3.buffers.size());
    subscriber3.subscription.request(2);
    assertEquals(5, subscriber1.buffers.size());
    assertEquals(5, subscriber2.buffers.size());
    assertEquals(5, subscriber3.buffers.size());
    assertEquals(buffers.get(0), subscriber1.buffers.get(0));
    assertEquals(buffers.get(1), subscriber1.buffers.get(1));
    assertEquals(buffers.get(2), subscriber1.buffers.get(2));
    assertEquals(buffers.get(3), subscriber1.buffers.get(3));
    assertEquals(buffers.get(4), subscriber1.buffers.get(4));
    assertEquals(buffers.get(0), subscriber2.buffers.get(0));
    assertEquals(buffers.get(1), subscriber2.buffers.get(1));
    assertEquals(buffers.get(2), subscriber2.buffers.get(2));
    assertEquals(buffers.get(3), subscriber2.buffers.get(3));
    assertEquals(buffers.get(4), subscriber2.buffers.get(4));
    assertEquals(buffers.get(0), subscriber3.buffers.get(0));
    assertEquals(buffers.get(1), subscriber3.buffers.get(1));
    assertEquals(buffers.get(2), subscriber3.buffers.get(2));
    assertEquals(buffers.get(3), subscriber3.buffers.get(3));
    assertEquals(buffers.get(4), subscriber3.buffers.get(4));
  }

  @Test
  public void testWriteQueueFullAndDrainDefaultQueueSize() throws Exception {
    ReactiveWriteStream rws = ReactiveWriteStream.writeStream();
    testWriteQueueFullAndDrain(rws, ReactiveWriteStream.DEFAULT_WRITE_QUEUE_MAX_SIZE);
  }

  @Test
  public void testWriteQueueFullAndDrain() throws Exception {
    ReactiveWriteStream rws = ReactiveWriteStream.writeStream();
    int size = ReactiveWriteStream.DEFAULT_WRITE_QUEUE_MAX_SIZE * 2;
    rws.setWriteQueueMaxSize(size);
    testWriteQueueFullAndDrain(rws, size);
  }

  private void testWriteQueueFullAndDrain(ReactiveWriteStream rws, int writeQueueMaxSize) throws Exception {
    MySubscriber subscriber = new MySubscriber();
    rws.subscribe(subscriber);
    Buffer buff = TestUtils.randomBuffer(writeQueueMaxSize - 1);
    rws.write(buff);
    assertFalse(rws.writeQueueFull());
    Buffer buff2 = TestUtils.randomBuffer(100);
    rws.write(buff2);
    assertTrue(rws.writeQueueFull());
    rws.drainHandler(v -> {
      assertFalse(rws.writeQueueFull());
      testComplete();
    });
    subscriber.subscription.request(2);
    await();
  }


  class MySubscriber implements Subscriber<Buffer> {

    List<Buffer> buffers = new ArrayList<>();
    Subscription subscription;

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
    }

    @Override
    public void onNext(Buffer buffer) {
      buffers.add(buffer);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
  }
}
