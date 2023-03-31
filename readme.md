# Reactive Streams specification in Java


## Introduction

_Reactive Streams_ is a cross-platform specification for processing a possibly unbounded sequence of events across asynchronous boundaries (threads, actors or network-connected computers) with non-blocking backpressure. _Backpressure_ is application-level flow control from the event consumer to the event producer to control its emission rate.

The main purpose of Reactive Streams is to let the subscriber control how quickly the publisher produces events. This is implemented by switching between _push_ and _pull_ models automatically based on consumption rate. In Reactive Streams consumers can use bounded buffers of known size without the danger of memory overflow.

![stream diagram](/images/stream_diagram.png)

The Reactive Streams specification is already implemented in the various programming platforms (.NET, JVM, JavaScript) and network protocols (RSocket). In the Java ecosystem, reactive threads are supported in particular (listed alphabetically):



* general-purpose frameworks (Eclipse Vert.x, Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, Parallel Universe Quasar, SmallRye Mutiny)
* web frameworks (Lightbend Play Framework, Oracle Helidon, Pivotal WebFlux, Ratpack)
* relational and non-relational databases (Apache Cassandra, Elasticsearch, MongoDB, PostgreSQL)
* message brokers (Apache Kafka, Pivotal RabbitMQ/AMQP)
* cloud providers (AWS SDK for Java 2.0)
* network protocols (RSocket)


## Protocol evolution

When transmitting items from the producer to the consumer, the goal is to transmit _all_ items with minimal latency and maximum throughput.

<sub>Latency is the time between the generation of an item at the producer and its arrival to the consumer. Throughput is the number of items sent from producer to consumer per unit of time.</sub>

However, the producer and the consumer may have limitations that can prevent the best performance from being achieved:



* The consumer can be slower than the producer
* The producer may not be able to stop emitting items that the consumer does not have time to process, or reduce the rate at which they are emitted
* The consumer may not be able to skip items that he does not have time to process
* The producer and the consumer may have a limited amount of memory for items buffering and CPU cores for asynchronous items processing
* The communication channel between the producer and the consumer may have a limited bandwidth

There are several patterns for sequential items processing that allows to bypass some or most of the above limitations:



* Iterator
* Observer
* Reactive Extensions
* Reactive Streams

These patterns fall into two groups: _pull_ models (in which the consumer determines when it requests items from the producer) and _push_ models (in which the producer determines when it sends items to the consumer).


### Iterator

In the Iterator pattern, the consumer synchronously _pulls_ items from the producer one by one. The producer sends an item only when the consumer requests it. If the producer has no item at the time of the request, it sends an empty response.

![Iterator](/images/Iterator.png)

Pros:



* The consumer can start the exchange at any time
* The consumer can not request the next item if he has not yet processed the previous one
* The consumer can stop the exchange at any time

Cons:



* The latency may not be optimal due to incorrectly chosen pulling period (too long pulling period leads to high latency, too short pulling period wastes CPU and I/O resources)
* The throughput is not optimal because it takes one request-response to send each item
* The consumer can not determine if the producer is done emitting items

When using the Iterator pattern that transmits items one at a time, latency and throughput are often unsatisfactory. To improve these parameters with minimal changes, the same Iterator pattern is often used, which transmits items in batches of fixed or variable size.

![Iterator with batching](/images/Iterator_with_batching.png)

Pros:



* Throughput increases as the number of request-responses decreases from one for _each_ item to one for _all_ items in a batch

Cons:



* The latency increases because the producer needs more time to emit more items
* If the batch size is too large, it may not fit in the memory of the producer or the consumer
* If the consumer wants to stop processing, he can do so no sooner than he receives the entire items batch


### Observer

In the Observer pattern, one or many consumers subscribe to the producer's events. The producer asynchronously _pushes_ events to all subscribed consumers as soon as they become available. The consumer can unsubscribe from the producer at any time if they do not need further events.

![Observer](/images/Observer.png)

Pros:



* The consumer can start the exchange at any time
* The consumer can stop the exchange at any time
* The latency is lower than in synchronous _pull_ models because the producer sends events to the consumer as soon as they become available

Cons:



* A slower consumer may be overwhelmed by the stream of events from a faster producer
* The consumer cannot determine if the producer has finished emitting events
* Implementing a concurrent producer can be non-trivial


### Reactive Extensions

Reactive Extensions (ReactiveX) is a family of multi-platform frameworks for handling synchronous or asynchronous events streams, originally created by Erik Meijer at Microsoft.

<sub>The implementation of Reactive Extensions for Java is the Netflix RxJava framework.</sub>

In simplified terms, Reactive Extensions can be thought of as a combination of Observer and Iterator patterns and functional programming. From the Observer pattern they took the ability of the consumer to subscribe to producer events. From the Iterator pattern they took the ability to handle event streams of three types (data, error, completion). From functional programming they took the ability to handle event streams with chained methods (transform, filter, combine, etc.).

![Reactive Extensions](/images/Reactive_Extensions.png)

Just as the Iterator pattern has synchronous _pull_ operations to handle data, errors, and completion, Reactive Extensions has methods to perform similar asynchronous _push_ operations.


<table>
  <tr>
   <td>
   </td>
   <td>Iterator (synchronous <em>pull</em>)
   </td>
   <td>Observable (asynchronous <em>push</em>)
   </td>
  </tr>
  <tr>
   <td>data event  
   </td>
   <td><em>T next()</em>
   </td>
   <td><em>onNext(T)</em>
   </td>
  </tr>
  <tr>
   <td>error event 
   </td>
   <td><em>throws Exception </em>
   </td>
   <td><em>onError(Exception)</em>
   </td>
  </tr>
  <tr>
   <td>completion event
   </td>
   <td><em>!hasNext()</em>
   </td>
   <td><em>onCompleted()</em>
   </td>
  </tr>
</table>


Pros:



* The consumer can start the exchange at any time
* The consumer can stop the exchange at any time
* The consumer can determine when the producer has finished emitting events
* The latency is lower than in synchronous _pull_ models because the producer sends events to the consumer as soon as they become available
* The consumer can handle the stream of events of the three types (data, error, completion) uniformly
* Handling event streams with chained methods can be easier than handling them with nested event handlers

Cons:



* A slower consumer may be overwhelmed by the stream of events from a faster producer
* Implementing a concurrent producer can be non-trivial


### Reactive Streams

Reactive Streams is an initiative to provide a standard for asynchronous stream processing with non-blocking backpressure.

Reactive Streams is a further development of Reactive Extensions, which was developed in particular to solve the problem of overflow of a slower consumer with a stream of events from a faster producer. In simplified terms, Reactive Streams can be thought of as a combination of Reactive Extensions and batching.

The consumer has the ability to inform the producer of the amount of events he would like to receive. This algorithm makes it possible to create streams which work equally efficiently regardless of whether the producer is faster than the consumer or vice versa, or even when performance of the producer or the consumer changes over time.

![Reactive Streams](/images/Reactive_Streams.png)

Pros:



* The consumer can start the exchange at any time
* The consumer can stop the exchange at any time
* The  consumer can determine when the producer has finished the evens generation
* The latency is lower than in synchronous _pull_ models because the producer sends events to the consumer as soon as they become available
* The consumer can handle the stream of events of the three types (data, error, completion) uniformly
* Handling event streams with chained methods can be easier than handling them with nested event handlers
* The consumer can request events from the producer depending on its demand

Cons:



* Implementing a concurrent producer can be non-trivial


## Backpressure

There are several solutions for situations where the consumer processes events slower than the producer emits them. This is not a problem for _pull_ models because the consumer is the initiator of the exchange. In _push_ models, the producer usually has no way to determine the rate of sending events, so the consumer may receive more events than he is able to handle. This performance mismatch can be resolved by backpressure in the consumer or in the producer.

There are several ways to deal with the backpressure on the consumer side:



* buffer events in a bounded buffer
* drop events
* drop events _and_ request the producer to resend them by their identifiers

In this situation, the choice is either to lose the events or to introduce additional I/O operations to resend them.

Backpressure is a solution in reactive streams to the problem of achieving maximum throughput with a slower consumer on the producer side _without_ losing messages. To start sending events from the producer, the consumer _pulls_ the number of events it wants to receive. Only then does the producer send events to the consumer, the producer never sends events on its own. If the consumer is deliberately faster than the producer, he can work in the _push_ model and request all events immediately after subscribing. If the consumer is deliberately slower than the producer, he can work in the _pull_ model and request the next events only after the previous ones have been processed.

Thus, the model in which reactive streams operate can be described as a _dynamic push/pull_ model. Reactive stream switches between _push_ and _pull_ models depending on whether or not the consumer is able to receive data events at the speed of the producer. This model works effectively if the producer is faster than the consumer or vice versa, or if this relationship can change unpredictably over time.

There are several ways to deal with the backpressure on the producer :



* pause event generation, if possible

otherwise:



* buffer events in a bounded buffer
* block the producer
* drop events
* cancel the events stream

Backpressure shifts the overflow problem to the producer side, where it is supposed to be easier to solve. However, depending on the nature of the events and the implementation of the producer and the consumer, there may be better solutions than backpressure, such as simply dropping the events.


## The Reactive Streams specification

Reactive Streams is a [specification](https://www.reactive-streams.org/) to provide a standard for asynchronous stream processing with non-blocking backpressure for various runtime environments (JVM, .NET and JavaScript) as well as network protocols. The Reactive Streams specification is created by engineers from Kaazing, Lightbend, Netflix, Pivotal, Red Hat, Twitter and others.

The specification describes the concept of _reactive stream_ which has the following features:



* reactive streams are potentially _unbounded_: they can handle zero, one, many, or an infinite number of events.
* reactive streams are _sequenced_: consumers process events in the same order in which they were emitted by the producer.
* reactive streams are _asynchronous_: they can use computing resources (CPU cores for one computer) for parallel processing in separate stream components.
* reactive streams are _non-blocking_: they do not waste computing resources if the rates of the producer and the consumer are different
* reactive streams use _mandatory backpressure_: consumers can request events from the producer according to their consumption rate.
* reactive streams use _bounded buffers_: they can be implemented without unbounded buffers which can lead to out-of-memory errors

The Reactive Streams [specification for the JVM](https://github.com/reactive-streams/reactive-streams-jvm) (the latest version 1.0.4 was released on May 26th, 2022) contains the [textual specification](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.4/README.md#specification) and the Java [API](https://www.reactive-streams.org/reactive-streams-1.0.4-javadoc) which contains four interfaces that must be implemented according to this specification. There is also the [Technology Compatibility Kit](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) (TCK), a standard test suite for conformance testing of implementations.

The Reactive Streams specification was created after several mature but incompatible implementations of Reactive Streams already existed. Therefore, the specification is currently limited and contains only low-level APIs. Application developers should use this specification to provide _interoperability_ between existing implementations. To have high-level functional APIs (transform, filter, combine, etc.) application developers should use implementations of this specification (Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, etc.) by their native APIs.


## The Reactive Streams API

The Reactive Streams API consists of the following interfaces, which are located in the _org.reactivestreams_ package:



* _Publisher&lt;T>_: a producer of data and control events
* _Subscriber&lt;T>_: a consumer of events
* _Subscription_: a connection that links a Publisher and a Subscriber
* _Processor&lt;T,R>_: a processor of events that acts as both a Subscriber and a Publisher

![Reactive Streams API](/images/Reactive_Streams_API.png)


### Publisher

The [Publisher](https://github.com/reactive-streams/reactive-streams-jvm#1-publisher-code) interface represents a producer of a potentially unbounded number of sequenced data and control events. A Publisher produces events according to the _demand_ received from one or many Subscribers.

<sub><em>Demand</em> is the aggregated number of elements requested by a Subscriber which is yet to be fulfilled by the Publisher.</sub>


```
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```


This interface has the following method:



* the _Publisher#subscribe(Subscriber)_ method requests the Publisher to start sending events to the Subscriber


### Subscriber

The [Subscriber](https://github.com/reactive-streams/reactive-streams-jvm#2-subscriber-code) interface represents a consumer of events. Multiple Subscribers can subscribe and unsubscribe from a Producer at various points in time. A Subscriber can subscribe only once to a single Publisher.


```
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
```


This interface has the following methods:



* the _onSubscribe(Subscription)_ method is invoked when the Producer accepted a new Subscription
* the _onNext(T)_ method is invoked on each received item
* the _onError(Throwable)_ method is invoked at erroneous completion
* the _onComplete()_ method is invoked on successful completion


### Subscription

The [Subscription](https://github.com/reactive-streams/reactive-streams-jvm#3-subscription-code) interface represents a connection between a Publisher and a Subscriber. Through a Subscription, the Subscriber can request items from the Publisher or cancel the connection at any time. Subscription can be used only once and only by the Subscriber who created it.


```
public interface Subscription {
    public void request(long n);
    public void cancel();
}
```


This interface has the following methods:



* the _request(long)_ method adds the given number of items to the unfulfilled demand for this Subscription
* the _cancel()_ method requests the Publisher to stop sending items and clean up resources


### Processor

The [Processor](https://github.com/reactive-streams/reactive-streams-jvm#4processor-code) interface represents a processing stage, which is both a Subscriber and a Publisher and is subject to the contracts of both. The Processor interface is designed to implement intermediate stream operations (transform, filter, combine, etc.).


```
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```



## The Reactive Streams workflow

![Reactive Streams workflow](/images/Reactive_Streams_workflow.png)

When a Subscriber wants to start receiving events from a Publisher, it calls the _Publisher#subscribe(Subscriber)_ method and passes itself as a parameter. If the Publisher accepts the request, it creates a new Subscription object and invokes the _Subscriber#onSubscribe(Subscription)_ method with it. If the Publisher rejects the request or otherwise fails (for example, it has already subscribed), it invokes the _Subscriber#onError(Throwable)_ method.

Once the connection between Publisher and Subscriber is established through the Subscription object, the Subscriber can request events and the Publisher can emit them. When the Subscriber wants to receive events, it calls the _Subscription#request(long)_ method with a number of  items requested. Typically, the first such call occurs in the _Subscriber#onSubscribe_ method. If the Subscriber wants to stop receiving events, it calls the _Subscription#cancel()_ method. After this method is called, the Subscriber can continue to receive events to meet the previously requested demand. A canceled Subscription does not receive Subscriber#onComplete() or Subscriber#onError(Throwable) events.

The Producer sends each requested event by calling the _Subscriber#onNext(T)_ method only in response to a previous request by the _Subscription#request(long)_ method, but never by itself. A Publisher can send fewer events than is requested if the stream ends, but then must call either the _Subscriber#onError(Throwable)_ or _Subscriber#onComplete()_ methods. After invocation of _Subscriber#onError(Throwable)_ or _Subscriber#onComplete()_ events, the current Subscription will not send any other events to the Subscriber.

When there are no more events, the Publisher completes the Subscription normally by calling the _Subscriber#onCompleted()_ method. When an unrecoverable exception occurs in the Publisher, it completes the Subscription exceptionally by calling the _Subscriber#onError(Throwable)_ method.

<sub>To make a reactive stream <em>push</em>-based, a Consumer can call the <em>Subscription#request(long)</em> method once with parameter Long.MAX_VALUE . To make a reactive stream <em>pull</em>-based, a Consumer can call the <em>Subscription#request(long)</em> method with parameter <em>1</em> every time it is ready to handle the next event.</sub>


## The JDK Flow API

Reactive Streams started to be supported in JDK 9 in the form of the Flow API. The _java.util.concurrent.Flow_ class contains the _Publisher_, _Subscriber_, _Subscription_, _Processor_ nested static interfaces, which are 100% semantically equivalent to their respective Reactive Streams counterparts. Reactive Streams contains the _org.reactivestreams.FlowAdapters _class, which is a bridge between the Reactive Streams API in the _org.reactivestreams_ package and the JDK Flow API in the _java.util.concurrent.Flow_ class. The only implementation of the Reactive Streams specification that JDK provides so far is the _java.util.concurrent.SubmissionPublisher_ class that implements the _Publisher_ interface.


### SubmissionPublisher

The [SubmissionPublisher](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/SubmissionPublisher.htm) class is an implementation of the _Publisher_ interface, which is compliant with the Reactive Streams specification. The _SubmissionPublisher_ class is implemented using an executor (to send events asynchronously to multiple consumers) and a bounded buffer for each consumer (to smooth out temporary disparities in producer and consumer speeds).

<sub>Normally the <em>SubmissionPublisher</em> class uses <em>ForkJoinPool.commonPool()</em> as an executor and <em>Flow.defaultBufferSize() = 256</em> as the maximum buffer capacity. </sub>

According to the architecture of Reactive Streams, the _SubmissionPublisher_ class can use different backpressure strategies:



* _buffering_: for each Subscriber, the Publisher creates a Subscription that contains a bounded buffer with an initial minimum size that can be expanded to the maximum buffer capacity
* _throttling_: during the emitting, the Publisher can use the _estimateMaximumLag_ (an estimate of the maximum number of items produced but not yet consumed among all subscribers) and _estimateMinimumDemand_ (an estimate of the minimum number of items requested but not yet produced among all subscribers) methods to predict the buffers saturation
* _blocking_: if the Publisher uses the _submit_ method to send items to Subscribers, when the buffer is full, this method blocks until there is free space in the buffer
* _dropping_ and _retrying_: if the Publisher uses the _offer_ method to send items to the Subscribers, when the buffer becomes full, this method can drop items (immediately or with a timeout) with or without the possibility to retry once

There are two ways for consumers to subscribe to the _SubmissionPublisher_ class: universal and specialized.

The universal way is to use the inherited _Publisher#subscribe(Subscriber)_ method. This solution allows the use of the standard Reactive Streams workflow of the _Subscriber_ class with its _onSubscribe_, _onNext_, _onError_, _onComplete_ methods.

The specialized way is to use the _SubmissionPublisher#consume(Consumer)_ method which returns a _CompletableFuture&lt;Void>_ result. This method is designed for a situation where it is known in advance that the consumer can handle the event stream emitted by the producer and no backpressure is required. Calling this method creates an implicit subscriber, which immediately calls the _Subscription#request(Long.MAX_VALUE)_ method to start the exchange in the _push_ model. The invocations of the _Consumer#accept_ method takes place in the _Consumer#onNext_ of this implicit subscriber. The _CompletableFuture_ result allows the caller to [determine](https://github.com/aliakh/demo-java-completablefuture/blob/master/readme.md) whether the reactive stream is completed normally by the producer or cancel the subscription itself.


## Conclusion

Reactive streams take a proper place among other parallel and concurrent Java frameworks. Before they appeared in the JDK, there were slightly related CompletableFuture and Stream APIs. CompletableFuture uses the _push_ model but supports asynchronous computations of a single value. Stream supports sequential or parallel computations of multiple values, but uses the _pull_ model. Reactive streams have taken a vacant place that supports synchronous or asynchronous computations of multiple values and can dynamically switch between the _push_ and _pull_ models. Reactive streams are suitable for processing possible unbounded sequences of events with unpredictable rates, such as user mouse and keyboard events, sensor events, latency-bound I/O events from file or network etc.

Application developers should not implement the interfaces of the Reactive Streams specification themselves. The specification is complex enough, especially in concurrent Publisher-Subscriber contracts, to be implemented correctly. Also the specification does not contain APIs for intermediate stream operations. They should use stream stages (producers, processors, consumers) from existing frameworks and use the Reactive Streams API only to connect them together. Then application developers should use the much richer native framework APIs.
