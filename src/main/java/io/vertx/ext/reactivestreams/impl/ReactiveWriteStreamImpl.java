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

import io.vertx.core.*;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.net.NetSocketInternal;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveWriteStreamImpl<T> implements ReactiveWriteStream<T> {

  private final Set<SubscriptionImpl> subscriptions = ConcurrentHashMap.newKeySet();
  private final Queue<Item<T>> pending = new ArrayDeque<>();
  private Handler<Void> drainHandler;
  private int writeQueueMaxSize = DEFAULT_WRITE_QUEUE_MAX_SIZE;
  protected final ContextInternal ctx;
  private boolean closed;

  public ReactiveWriteStreamImpl(Vertx vertx) {
    ctx = (ContextInternal) vertx.getOrCreateContext();
  }

  private void checkClosed() {
    if (closed) {
      throw new IllegalStateException("Closed");
    }
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    Objects.requireNonNull(subscriber);
    SubscriptionImpl sub;
    synchronized (this) {
      checkClosed();
      sub = new SubscriptionImpl(subscriber);
      if (!subscriptions.add(sub)) {
        throw new IllegalStateException("1.10 Cannot subscribe multiple times with the same subscriber.");
      }
    }
    invokeOnSubscribe(subscriber, sub);
  }

  private void invokeOnSubscribe(Subscriber<? super T> subscriber, SubscriptionImpl sub) {
    if (ctx.equals(ContextInternal.current())) {
      try {
        subscriber.onSubscribe(sub);
      } catch (Throwable t) {
        signalError(sub.subscriber, t);
      }
    } else {
      ctx.runOnContext(v -> {
        invokeOnSubscribe(subscriber, sub);
      });
    }
  }

  @Override
  public synchronized Future<Void> write(T data) {
    checkClosed();
    Promise<Void> promise = ctx.promise();
    pending.add(new Item<>(data, promise));
    checkSend();
    return promise.future();
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
  public Future<Void> end() {
    close();
    return ctx.succeededFuture();
  }

  @Override
  public ReactiveWriteStream<T> close() {
    synchronized (this) {
      if (closed) {
        return this;
      }
      closed = true;
      complete();
      subscriptions.clear();
      Future<Void> closedFut = Future.failedFuture(NetSocketInternal.CLOSED_EXCEPTION);
      for (Item<T> item: pending) {
        Completable<Void> handler = item.handler;
        if (handler != null) {
          ctx.runOnContext(v -> {
            if (closedFut.succeeded()) {
              handler.succeed(closedFut.result());
            } else {
              handler.fail(closedFut.cause());
            }
          });
        }
      }
      pending.clear();
    }
    return this;
  }

  private synchronized void checkSend() {
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

  private void sendToSubscribers(Item<T> item) {
    for (SubscriptionImpl sub: subscriptions) {
      onNext(ctx, sub.subscriber, item.value);
    }
    if (item.handler != null) {
      item.handler.succeed();
    }
  }

  protected void onNext(Context context, Subscriber<? super T> subscriber, T data) {
    context.runOnContext(v -> {
      try {
        subscriber.onNext(data);
      } catch (Throwable t) {
        signalError(subscriber, t);
      }
    });
  }

  public class SubscriptionImpl implements Subscription {

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
          signalError(subscriber, new IllegalStateException("3.17 Subscriber has more then Long.MAX_VALUE (2^63-1) currently pending."));
        } else {
          checkSend();
        }
      } else {
        signalError(subscriber, new IllegalArgumentException("3.9 Subscriber cannot request less then 1 for the number of elements."));
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

  private void signalError(Subscriber<? super T> subscriber, Throwable error) {
    subscriptions.removeIf(sub -> sub.subscriber == subscriber);
    subscriber.onError(error);
  }

  static class Item<T> {
    final T value;
    final Completable<Void> handler;
    Item(T value, Completable<Void> handler) {
      this.value = value;
      this.handler = handler;
    }
  }
}
