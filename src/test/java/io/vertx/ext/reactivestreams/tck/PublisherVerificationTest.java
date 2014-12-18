package io.vertx.ext.reactivestreams.tck;

import io.vertx.ext.reactivestreams.ReactiveWriteStream;
import io.vertx.test.core.TestUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;


/**
 * @author <a href="mailto:nscavell@redhat.com">Nick Scavelli</a>
 */
public class PublisherVerificationTest extends PublisherVerification<Object> {

  private static final long DEFAULT_TIMEOUT = 300L;
  private static final long DEFAULT_GC_TIMEOUT = 1000L;

  public PublisherVerificationTest() {
    super(new TestEnvironment(DEFAULT_TIMEOUT), DEFAULT_GC_TIMEOUT);
  }

  @Override
  public Publisher<Object> createPublisher(long elements) {
    ReactiveWriteStream<Object> rws;
    if (elements >= Integer.MAX_VALUE) {
      rws = new FiniteReactiveWriteStream<>(elements);
    } else {
      rws = new FiniteReactiveWriteStream<>(elements);
      for (long i = 0; i < elements; i++) {
        rws.write(TestUtils.randomBuffer(10));
      }
    }

    return rws;
  }

  @Override
  public Publisher<Object> createErrorStatePublisher() {
    return s -> s.onError(new RuntimeException());
  }
}
