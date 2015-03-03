/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.ext.reactivestreams.tck;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.ext.reactivestreams.impl.ReactiveWriteStreamImpl;
import org.reactivestreams.Subscriber;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 */
public class FiniteReactiveWriteStream<T> extends ReactiveWriteStreamImpl<T> {

  private Map<Subscriber<? super T>, AtomicLong> subs = new WeakHashMap<>();
  private final long elements;

  public FiniteReactiveWriteStream(Vertx vertx, long elements) {
    super(vertx);
    this.elements = elements;
  }

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    subs.put(subscriber, new AtomicLong(elements));
    super.subscribe(subscriber);
  }

  @Override
  protected void onNext(Context context, Subscriber<? super T> subscriber, T data) {
    AtomicLong count = subs.get(subscriber);
    if (count == null) return; // Means we already completed it

    long remaining = count.decrementAndGet();
    super.onNext(context, subscriber, data);
    if (remaining == 0) {
      close();
    }
  }
}
