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

package io.vertx.ext.reactivestreams;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.reactivestreams.impl.ReactiveReadStreamImpl;
import org.reactivestreams.Subscriber;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ReactiveReadStream<T> extends ReadStream<T>, Subscriber<T> {

  static final long DEFAULT_BATCH_SIZE = 4L;

  static <T> ReactiveReadStream<T> readStream() {
    return readStream(DEFAULT_BATCH_SIZE);
  }

  static <T> ReactiveReadStream<T> readStream(long batchSize) {
    return new ReactiveReadStreamImpl<>(batchSize);
  }

  @Override
  ReactiveReadStream<T> exceptionHandler(Handler<Throwable> handler);

  @Override
  ReactiveReadStream<T> handler(Handler<T> handler);

  @Override
  ReactiveReadStream<T> pause();

  @Override
  ReactiveReadStream<T> resume();

  @Override
  ReactiveReadStream<T> endHandler(Handler<Void> endHandler);
}
