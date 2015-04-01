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

package io.vertx.ext.reactivestreams.tck;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.reactivestreams.ReactiveReadStream;
import io.vertx.ext.reactivestreams.impl.ReactiveReadStreamImpl;
import org.junit.Ignore;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.SubscriberWhiteboxVerification;
import org.reactivestreams.tck.TestEnvironment;

/*
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
@Ignore
public class SubscriberWhiteboxVerificationTest extends SubscriberWhiteboxVerification<Buffer> {


  public SubscriberWhiteboxVerificationTest() {
    super(new TestEnvironment());
  }

  @Override
  public Subscriber<Buffer> createSubscriber(WhiteboxSubscriberProbe<Buffer> probe) {

    // return YOUR subscriber under-test, with additional WhiteboxSubscriberProbe instrumentation
    return new ReactiveReadStreamImpl<Buffer>(ReactiveReadStream.DEFAULT_BATCH_SIZE) {

      @Override
      public void onSubscribe(final Subscription s) {

        super.onSubscribe(s);

        // in addition to normal Subscriber work that you're testing,
        // register a SubscriberPuppet, to give the TCK control over demand generation and cancelling
        probe.registerOnSubscribe(new SubscriberPuppet() {

          @Override
          public void triggerRequest(long n) {
            s.request(n);
          }

          @Override
          public void signalCancel() {
            s.cancel();
          }
        });


      }

      @Override
      public void onNext(Buffer value) {

        // in addition to normal Subscriber work that you're testing, register onNext with the probe
        probe.registerOnNext(value);
        super.onNext(value);
      }

      @Override
      public void onError(Throwable cause) {

        // in addition to normal Subscriber work that you're testing, register onError with the probe
        probe.registerOnError(cause);
        super.onError(cause);
      }

      @Override
      public void onComplete() {

        // in addition to normal Subscriber work that you're testing, register onComplete with the probe
        probe.registerOnComplete();
        super.onComplete();
      }

      // Need to override this to drop the tokens check or the test won't pass
      @Override
      protected void checkUnsolicitedTokens() {
        // NO OP
      }
    };
  }

  @Override
  public Buffer createElement(int element) {
    return Buffer.buffer("element" + element);
  }

}
