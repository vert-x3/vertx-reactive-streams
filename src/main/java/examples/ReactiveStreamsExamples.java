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

package examples;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.Pump;
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class ReactiveStreamsExamples {

  public void example1(HttpServerResponse response, Publisher<Buffer> otherPublisher) {

    ReactiveReadStream<Buffer> rrs = ReactiveReadStream.readStream();

    // Subscribe the read stream to the publisher
    otherPublisher.subscribe(rrs);

    // Pump from the read stream to the http response
    Pump pump = Pump.pump(rrs, response);

    pump.start();

  }

  public void example2(Vertx vertx, HttpServerRequest request, Subscriber<Buffer> otherSubscriber) {

    ReactiveWriteStream<Buffer> rws = ReactiveWriteStream.writeStream(vertx);

    // Subscribe the other subscriber to the write stream
    rws.subscribe(otherSubscriber);

    // Pump the http request to the write stream
    Pump pump = Pump.pump(request, rws);

    pump.start();
  }
}
