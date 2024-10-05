module io.vertx.reactivestreams.tests {
  requires io.vertx.core;
  requires io.vertx.core.tests;
  requires io.vertx.reactivestreams;
  requires junit;
  requires org.reactivestreams;
  requires org.reactivestreams.tck;

  exports io.vertx.ext.reactivestreams.test;
  exports io.vertx.ext.reactivestreams.tck;
}
