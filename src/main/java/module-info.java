open module io.vertx.reactivestreams {

  requires static io.vertx.docgen;

  requires io.vertx.core;
  requires org.reactivestreams;

  exports io.vertx.ext.reactivestreams;
  exports io.vertx.ext.reactivestreams.impl to io.vertx.reactivestreams.tests;

}
