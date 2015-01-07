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
public class ReactiveWriteStreamImpl<T> implements ReactiveWriteStream<T> {

  private Set<SubscriptionImpl> subscriptions = new HashSet<>();
  private final Queue<T> pending = new ArrayDeque<>();
  private Handler<Void> drainHandler;
  private int writeQueueMaxSize = DEFAULT_WRITE_QUEUE_MAX_SIZE;
  private final Thread thread;

  public ReactiveWriteStreamImpl() {
    this.thread = Thread.currentThread();
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    checkThread();
    SubscriptionImpl sub = new SubscriptionImpl(subscriber);
    subscriptions.add(sub);
    subscriber.onSubscribe(sub);
  }

  @Override
  public ReactiveWriteStream<T> write(T data) {
    checkThread();
    pending.add(data);
    checkSend();
    return this;
  }

  @Override
  public ReactiveWriteStream<T> setWriteQueueMaxSize(int maxSize) {
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
    return pending.size() >= writeQueueMaxSize;
  }

  @Override
  public ReactiveWriteStream<T> drainHandler(Handler<Void> handler) {
    checkThread();
    this.drainHandler = handler;
    return this;
  }

  @Override
  public ReactiveWriteStream<T> exceptionHandler(Handler<Throwable> handler) {
    checkThread();
    return this;
  }

  private void checkThread() {
    if (Thread.currentThread() != thread) {
      throw new IllegalStateException("Wrong thread!");
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
      if (drainHandler != null && pending.size() < writeQueueMaxSize) {
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

  private void sendToSubscribers(T data) {
    for (SubscriptionImpl sub: subscriptions) {
      sub.subscriber.onNext(data);
    }
  }

  class SubscriptionImpl implements Subscription {

    Subscriber<? super T> subscriber;
    int tokens;

    SubscriptionImpl(Subscriber<? super T> subscriber) {
      this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
      checkThread();
      tokens += n;
      checkSend();
    }

    @Override
    public void cancel() {
      subscriptions.remove(this);
    }
  }
}
