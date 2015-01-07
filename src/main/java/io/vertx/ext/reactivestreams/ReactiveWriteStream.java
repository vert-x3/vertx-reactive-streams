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
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.reactivestreams.impl.ReactiveWriteStreamImpl;
import org.reactivestreams.Publisher;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

public interface ReactiveWriteStream<T> extends WriteStream<T>, Publisher<T> {

  static final int DEFAULT_WRITE_QUEUE_MAX_SIZE = 32;

  static <T> ReactiveWriteStream<T> writeStream() {
    return new ReactiveWriteStreamImpl<>();
  }

  @Override
  ReactiveWriteStream<T> exceptionHandler(Handler<Throwable> handler);

  @Override
  ReactiveWriteStream<T> write(T data);

  @Override
  ReactiveWriteStream<T> setWriteQueueMaxSize(int maxSize);

  @Override
  ReactiveWriteStream<T> drainHandler(Handler<Void> handler);

}
