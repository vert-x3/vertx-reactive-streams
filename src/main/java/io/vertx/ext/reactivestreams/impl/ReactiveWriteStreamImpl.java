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

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveWriteStreamImpl<T> implements ReactiveWriteStream<T> {

  private Set<SubscriptionImpl> subscriptions = new HashSet<>();
  private final Queue<T> pending = new ArrayDeque<>();
  private Handler<Void> drainHandler;
  private int writeQueueMaxSize = DEFAULT_WRITE_QUEUE_MAX_SIZE;
  private final Context ctx;
  private boolean closed;

  public ReactiveWriteStreamImpl(Vertx vertx) {
    ctx = vertx.getOrCreateContext();
  }

  private void checkClosed() {
    if (closed) {
      throw new IllegalStateException("Closed");
    }
  }

  @Override
  public synchronized void subscribe(Subscriber<? super T> subscriber) {
    checkClosed();
    Objects.requireNonNull(subscriber);

    SubscriptionImpl sub = new SubscriptionImpl(subscriber);
    if (subscriptions.add(sub)) {
      ctx.runOnContext(v -> {
        try {
          subscriber.onSubscribe(sub);
        } catch (Throwable t) {
          subscriber.onError(t);
        }
      });
    } else {
      throw new IllegalStateException("1.10 Cannot subscribe multiple times with the same subscriber.");
    }
  }

  @Override
  public synchronized ReactiveWriteStream<T> write(T data) {
    checkClosed();
    pending.add(data);
    checkSend();
    return this;
  }

  @Override
  public synchronized ReactiveWriteStream<T> setWriteQueueMaxSize(int maxSize) {
    checkClosed();
    if (writeQueueMaxSize < 1) {
      throw new IllegalArgumentException("writeQueueMaxSize must be >=1");
    }
    this.writeQueueMaxSize = maxSize;
    return this;
  }

  @Override
  public synchronized boolean writeQueueFull() {
    checkClosed();
    return pending.size() >= writeQueueMaxSize;
  }

  @Override
  public synchronized ReactiveWriteStream<T> drainHandler(Handler<Void> handler) {
    checkClosed();
    this.drainHandler = handler;
    return this;
  }

  @Override
  public synchronized ReactiveWriteStream<T> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public synchronized ReactiveWriteStream<T> close() {
    if (!closed) {
      complete();
      subscriptions.clear();
      pending.clear();
      closed = true;
    }
    return this;
  }

  private void checkSend() {
    if (!subscriptions.isEmpty()) {
      long availableTokens = getAvailable();
      long toSend = Math.min(availableTokens, pending.size());
      takeTokens(toSend);
      for (long i = 0; i < toSend; i++) {
        sendToSubscribers(pending.poll());
      }
      if (drainHandler != null && pending.size() < writeQueueMaxSize) {
        callDrainHandler();
      }
    }
  }

  private void callDrainHandler() {
    Handler<Void> dh = drainHandler;
    ctx.runOnContext(v -> dh.handle(null));
  }

  private long getAvailable() {
    long min = Long.MAX_VALUE;
    for (SubscriptionImpl subscription: subscriptions) {
      min = Math.min(subscription.tokens(), min);
    }
    return min;
  }

  private void takeTokens(long toSend) {
    for (SubscriptionImpl subscription: subscriptions) {
      subscription.takeTokens(toSend);
    }
  }

  private void complete() {
    for (SubscriptionImpl sub: subscriptions) {
      ctx.runOnContext(v -> sub.subscriber.onComplete());
    }
  }

  private void sendToSubscribers(T data) {
    for (SubscriptionImpl sub: subscriptions) {
      onNext(ctx, sub.subscriber, data);
    }
  }

  protected void onNext(Context context, Subscriber<? super T> subscriber, T data) {
    context.runOnContext(v -> {
      try {
        subscriber.onNext(data);
      } catch (Throwable t) {
        subscriber.onError(t);
      }
    });
  }

  private class SubscriptionImpl implements Subscription {

    private final Subscriber<? super T> subscriber;
    // We start at Long.MIN_VALUE so we know when we've requested more then Long.MAX_VALUE. See 3.17 of spec
    private final AtomicLong tokens = new AtomicLong(Long.MIN_VALUE);

    private SubscriptionImpl(Subscriber<? super T> subscriber) {
      this.subscriber = subscriber;
    }

    public long tokens() {
      return -(Long.MIN_VALUE - tokens.get());
    }

    public void takeTokens(long amount) {
      tokens.addAndGet(-amount);
    }

    @Override
    public void request(long n) {
      if (n > 0) {
        // More then Long.MAX_VALUE pending
        if (tokens.addAndGet(n) > 0) {
          subscriber.onError(new IllegalStateException("3.17 Subscriber has more then Long.MAX_VALUE (2^63-1) currently pending."));
        } else {
          checkSend();
        }
      } else {
        subscriber.onError(new IllegalArgumentException("3.9 Subscriber cannot request less then 1 for the number of elements."));
      }
    }

    @Override
    public void cancel() {
      subscriptions.remove(this);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      @SuppressWarnings("unchecked")
      SubscriptionImpl that = (SubscriptionImpl) o;

      return subscriber == that.subscriber;
    }

    @Override
    public int hashCode() {
      return subscriber.hashCode();
    }
  }
}
