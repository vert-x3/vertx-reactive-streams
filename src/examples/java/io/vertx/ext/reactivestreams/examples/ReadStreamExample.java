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

package io.vertx.ext.reactivestreams.examples;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReadStreamExample {

  public static void main(String[] args) {
    new ReadStreamExample().run();
  }

  public void run() {

    // ReadStream example

    WriteStream ws = null; // E.g. get writeStream from Vert.x HttpServerResponse

    ReactiveReadStream rrs = ReactiveReadStream.readStream();

    Publisher<Buffer> prod = null; // E.g. get Publisher from, say, Akka

    prod.subscribe(rrs);

    Pump pump = Pump.pump(rrs, ws);

    pump.start();

  }

  class ThirdPartyPublisher implements Publisher<Buffer> {

    class ThirdPartySubscription implements Subscription {

      @Override
      public void request(int i) {

      }

      @Override
      public void cancel() {

      }
    }

    @Override
    public void subscribe(Subscriber<Buffer> subscriber) {

    }
  }
}
