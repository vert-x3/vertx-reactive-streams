# Reactive Streams for Vert.x

"[Reactive Streams](http://www.reactive-streams.org/) is an initiative to provide a standard for asynchronous stream
processing with non-blocking back pressure on the JVM."

This Vert.x extension provides an implementation of reactive streams for Vert.x.

## `ReactiveReadStream`

We provide an implementation of the Vert.x `ReadStream` interface which also implements a reactive streams `Subscriber`. You can 
pass an instance of this to any reactive streams `Publisher` (e.g. a Publisher from Akka) and then you will be able
to read from that just like any other Vert.x `ReadStream` (e.g. use a `Pump` to pump it to a `WriteStream`.

## `ReactiveWriteStream`

We also provide an implementation of the Vert.x `WriteStream` interface which also implements a reactive streams
`Publisher`. You can take any reactive streams `Subscriber` (e.g. a Subscriber from Akka) and then you will be able
to write to it like any other Vert.x `WriteStream`. (e.g. use a `Pump` to pump it from a `ReadStream`).

You use `pause`, `resume`, and `writeQueueFull`, as normal to handle your back pressure. This is automatically translated
internally into the reactive streams method of propagating back pressure (requesting more items).




