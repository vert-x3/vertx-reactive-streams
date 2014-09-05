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

package io.vertx.ext.reactivestreams.impl;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveWriteStreamImpl implements ReactiveWriteStream {

  private Set<SubscriptionImpl> subscriptions = new HashSet<SubscriptionImpl>();
  private final Queue<Buffer> pending = new ArrayDeque<>();
  private Handler<Void> drainHandler;
  private int writeQueueMaxSize = DEFAULT_WRITE_QUEUE_MAX_SIZE;
  private int maxBufferSize = DEFAULT_MAX_BUFFER_SIZE;
  private int totPending;
  private final Thread thread;

  ReactiveWriteStreamImpl() {
    this.thread = Thread.currentThread();
  }

  @Override
  public void subscribe(Subscriber<Buffer> subscriber) {
    checkThread();
    SubscriptionImpl sub = new SubscriptionImpl(subscriber);
    subscriptions.add(sub);
    subscriber.onSubscribe(sub);
  }

  @Override
  public ReactiveWriteStreamImpl writeBuffer(Buffer data) {
    checkThread();
    if (data.length() > maxBufferSize) {
      splitBuffers(data);
    } else {
      pending.add(data);
    }
    totPending += data.length();
    checkSend();
    return this;
  }

  @Override
  public ReactiveWriteStreamImpl setWriteQueueMaxSize(int maxSize) {
    checkThread();
    if (writeQueueMaxSize < 1) {
      throw new IllegalArgumentException("writeQueueMaxSize must be >=1");
    }
    this.writeQueueMaxSize = maxSize;
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    checkThread();
    return totPending >= writeQueueMaxSize;
  }

  @Override
  public ReactiveWriteStreamImpl drainHandler(Handler<Void> handler) {
    checkThread();
    this.drainHandler = handler;
    return this;
  }

  @Override
  public ReactiveWriteStreamImpl exceptionHandler(Handler<Throwable> handler) {
    checkThread();
    return this;
  }

  @Override
  public ReactiveWriteStream setBufferMaxSize(int maxBufferSize) {
    checkThread();
    if (maxBufferSize < 1) {
      throw new IllegalArgumentException("maxBufferSize must be >=1");
    }
    this.maxBufferSize = maxBufferSize;
    return this;
  }

  private void checkThread() {
    if (Thread.currentThread() != thread) {
      throw new IllegalStateException("Wrong thread!");
    }
  }

  private void splitBuffers(Buffer data) {
    int pos = 0;
    while (pos < data.length() - 1) {
      int end = pos + Math.min(maxBufferSize, data.length() - pos);
      Buffer slice = data.slice(pos, end);
      pending.add(slice);
      pos += maxBufferSize;
    }
  }

  private void checkSend() {
    if (!subscriptions.isEmpty()) {
      int availableTokens = getAvailable();
      int toSend = Math.min(availableTokens, pending.size());
      takeTokens(toSend);
      for (int i = 0; i < toSend; i++) {
        sendToSubscribers(pending.poll());
      }
      if (drainHandler != null && totPending < writeQueueMaxSize) {
        drainHandler.handle(null);
      }
    }
  }

  private int getAvailable() {
    int min = Integer.MAX_VALUE;
    for (SubscriptionImpl subscription: subscriptions) {
      min = Math.min(subscription.tokens, min);
    }
    return min;
  }

  private void takeTokens(int toSend) {
    for (SubscriptionImpl subscription: subscriptions) {
      subscription.tokens -= toSend;
    }
  }

  private void sendToSubscribers(Buffer data) {
    boolean copy = subscriptions.size() > 1;
    for (SubscriptionImpl sub: subscriptions) {
      if (copy) {
        data = data.copy();
      }
      totPending -= data.length();
      sub.subscriber.onNext(data);
    }
  }

  class SubscriptionImpl implements Subscription {

    Subscriber<Buffer> subscriber;
    int tokens;

    SubscriptionImpl(Subscriber<Buffer> subscriber) {
      this.subscriber = subscriber;
    }

    @Override
    public void request(int i) {
      checkThread();
      tokens += i;
      checkSend();
    }

    @Override
    public void cancel() {
      subscriptions.remove(this);
    }
  }
}
