package io.vertx.ext.reactivestreams.tck;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import io.vertx.ext.reactivestreams.impl.ReactiveWriteStreamImpl;
import io.vertx.test.core.TestUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;


/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 */
public class PublisherVerificationTest extends PublisherVerification<Buffer> {

  private static final long DEFAULT_TIMEOUT = 300L;
  private static final long DEFAULT_GC_TIMEOUT = 1000L;

  private Vertx vertx;

  public PublisherVerificationTest() {
    super(new TestEnvironment(DEFAULT_TIMEOUT), DEFAULT_GC_TIMEOUT);
    this.vertx = Vertx.vertx();
  }

  @Override
  public Publisher<Buffer> createPublisher(long elements) {
    ReactiveWriteStream<Buffer> rws;
    rws = new FiniteReactiveWriteStream<>(vertx, elements);
    if (elements < Integer.MAX_VALUE) {
      for (long i = 0; i < elements; i++) {
        rws.write(TestUtils.randomBuffer(10));
      }
    }
    return rws;
  }

  @Override
  public Publisher<Buffer> createFailedPublisher() {

    return new ReactiveWriteStreamImpl<Buffer>(vertx) {
      @Override
      public void subscribe(Subscriber<? super Buffer> subscriber) {
        Context ctx = vertx.getOrCreateContext();
        super.subscribe(subscriber);
        ctx.runOnContext(v -> {
          // Now signal an error
          subscriber.onError(new RuntimeException("Can't subscribe subscriber: " + subscriber + ", because of reasons."));
        });
      }
    };
  }
}
