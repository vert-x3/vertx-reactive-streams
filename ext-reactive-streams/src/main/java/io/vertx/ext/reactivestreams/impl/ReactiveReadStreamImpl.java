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
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveReadStreamImpl implements ReactiveReadStream {

  private final int bufferRequestBatchSize;

  private Handler<Buffer> dataHandler;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;

  private Subscription subscription;
  private final Queue<Buffer> pending = new ArrayDeque<>();
  private boolean paused;
  private int tokens;
  private final Thread thread;

  ReactiveReadStreamImpl(int bufferRequestBatchSize) {
    this.bufferRequestBatchSize = bufferRequestBatchSize;
    thread = Thread.currentThread();
  }

  ReactiveReadStreamImpl() {
    this(DEFAULT_BUFFER_REQUEST_BATCH_SIZE);
  }

  public ReactiveReadStream handler(Handler<Buffer> handler) {
    checkThread();
    this.dataHandler = handler;
    if (dataHandler != null && !paused) {
      checkRequestTokens();
    }
    return this;
  }

  @Override
  public ReactiveReadStream pause() {
    checkThread();
    this.paused = true;
    return this;
  }

  @Override
  public ReactiveReadStream resume() {
    checkThread();
    this.paused = false;
    Buffer data;
    while ((data = pending.poll()) != null) {
      handleData(data);
    }
    checkRequestTokens();
    return this;
  }

  @Override
  public ReactiveReadStream endHandler(Handler<Void> endHandler) {
    checkThread();
    this.endHandler = endHandler;
    return this;
  }

  @Override
  public ReactiveReadStream exceptionHandler(Handler<Throwable> handler) {
    checkThread();
    this.exceptionHandler = handler;
    return this;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    checkThread();
    this.subscription = subscription;
  }

  @Override
  public void onNext(Buffer buffer) {
    checkThread();
    if (tokens == 0) {
      throw new IllegalStateException("Data received but wasn't requested");
    }
    handleData(buffer);
  }

  @Override
  public void onError(Throwable throwable) {
    checkThread();
    if (exceptionHandler != null) {
      exceptionHandler.handle(throwable);
    }
  }

  @Override
  public void onComplete() {
    checkThread();
    if (endHandler != null) {
      endHandler.handle(null);
    }
  }

  private void handleData(Buffer buffer) {
    if (paused) {
      pending.add(buffer);
    } else if (dataHandler != null) {
      dataHandler.handle(buffer);
      tokens--;
      checkRequestTokens();
    }
  }

  private void checkRequestTokens() {
    if (!paused && subscription != null && tokens == 0) {
      // We request in whole number of buffers, not in bytes
      tokens = bufferRequestBatchSize;
      subscription.request(bufferRequestBatchSize);
    }
  }

  private void checkThread() {
    if (Thread.currentThread() != thread) {
      throw new IllegalStateException("Wrong thread!");
    }
  }

}
