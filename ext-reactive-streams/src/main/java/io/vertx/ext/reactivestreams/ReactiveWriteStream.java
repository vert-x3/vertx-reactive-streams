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

import io.vertx.core.ServiceHelper;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.reactivestreams.spi.ReactiveWriteStreamFactory;
import org.reactivestreams.Publisher;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

public interface ReactiveWriteStream extends WriteStream<ReactiveWriteStream, Buffer>, Publisher<Buffer> {

  ReactiveWriteStream setBufferMaxSize(int maxBufferSize);

  static ReactiveWriteStream writeStream() {
    return factory.writeStream();
  }

  static final int DEFAULT_MAX_BUFFER_SIZE = 8 * 1024;

  static final int DEFAULT_WRITE_QUEUE_MAX_SIZE = 32 * 1024;

  static final ReactiveWriteStreamFactory factory = ServiceHelper.loadFactory(ReactiveWriteStreamFactory.class);

}
