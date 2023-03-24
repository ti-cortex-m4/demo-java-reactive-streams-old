
# Reactive Streams in Java


## Introduction

_Reactive Streams_ is a cross-platform specification for processing a possibly unbounded sequence of events across asynchronous boundaries (threads, processes or network-connected computers) with non-blocking back pressure. _Back pressure_ is application-level flow control from the event consumer to the event producer to scale the emitting of events in the producer in accordance with their demand from the consumer. Reactive streams are designed to achieve high throughput and low latency in sequential processing, where transferring events between asynchronous processing stages takes noticeable time.

![stream diagram](/images/stream_diagram.png)

The Reactive Streams specification is already implemented in the various programming platforms (.NET, JVM, JavaScript) and network protocols (RSocket). In the Java ecosystem, reactive threads are supported in particular:



* general-purpose frameworks (Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, Parallel Universe Quasar, SmallRye Mutiny)
* web frameworks (Eclipse Vert.x, Lightbend Play Framework, Oracle Helidon, Pivotal WebFlux, Ratpack)
* relational and non-relational databases (Apache Cassandra, Elasticsearch, MongoDB, PostgreSQL)
* message brokers (Apache Kafka, Pivotal RabbitMQ/AMQP)
* cloud providers (AWS SDK for Java 2.0)

_Reactive streams_ is a concurrency framework compatible with the Reactive Streams specification, which was added in JDK 9. It consists of four nested interfaces in the _java.util.concurrent.Flow_ class (_Publisher_, _Subscriber_, _Subscription_, _Processor_) and a single implementation - the _SubmissionPublisher_ class which implements the _Flow.Publisher_ interface.


## Architecture

When transmitting items from the producer to the consumer, the goal is to transmit _all_ items with minimal latency and maximum throughput.

<sub>Latency is the time between the generation of an item at the producer and its arrival to the consumer. </sub>

<sub>Throughput is the number of items sent from producer to consumer per unit of time.</sub>

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
* The throutput is not optimal because it takes one request-response to send each item
* The consumer can not determine if the producer is done emitting items

When using the Iterator pattern that transmits items one at a time, latency and throutput are often unsatisfactory. To improve these parameters with minimal changes, the same Iterator pattern is often used, which transmits items in batches of fixed or variable size.

![Iterator with batching](/images/Iterator_with_batching.png)

Pros:



* Throutput increases as the number of request-responses decreases from one for _each_ item to one for _all_ items in a batch

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
   <td>Iterator (<em>pull</em>)
   </td>
   <td>Observable (<em>push</em>)
   </td>
  </tr>
  <tr>
   <td>data event  
   </td>
   <td>T next()
   </td>
   <td>onNext(T)
   </td>
  </tr>
  <tr>
   <td>error event 
   </td>
   <td>throws Exception 
   </td>
   <td>onError(Exception)
   </td>
  </tr>
  <tr>
   <td>completion event
   </td>
   <td>!hasNext()
   </td>
   <td>onCompleted()
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

Reactive Streams is an initiative to provide a standard for asynchronous stream processing with non-blocking back pressure.

Reactive Streams is a further development of Reactive Extensions, which was developed in particular to solve the problem of overflow of a slower consumer with a stream of events from a faster producer. In simplified terms, Reactive Streams can be thought of as a combination of Reactive Extensions and batching.

Еhe consumer has the ability to inform the producer of the amount of events he would like to receive. This algorithm makes it possible to create systems that work equally efficiently regardless of whether the producer is faster than the consumer or vice versa, or even when performance of the producer or the consumer changes over time.

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

## Back-pressure

Trying to achieve maximum throughput, you can come to a situation where the slower consumer will not be able to process all events from the faster producer. Typically it’s not a problem for the pull models because the consumer initiates the exchange.

In push models a producer generally does not have ways to identify the rate of sending data events. So a consumer may receive more data events that it is able to receive. Depends on the nature of the consumer, there can be different strategies, for example:



* buffer elements in a bounded buffer
* drop first or last elements

Reactive streams take a special place outside this classification because they can work both in “pull” and “push” models. To start an reactive stream, a consumer _pulls_ the number of data events it is able to receive. Only after receiving the number of requested events, the producer _pushes_ them to the consumer. If a consumer does not need data it does not request them, and the producer never sends events by its initiative. After receiving all the requested events, the producer can repeat the whole cycle again.

If a producer is slower than a consumer, then we have a simple scenario. Because we don’t have to slow down a producer, that in some cases is non-trivial or even impossible. The consumer can at any time request the number of data events it wants to receive, and the producer will send the data events as fast as it can. In this scenario the reactive stream operates in the ”push” mode, because the producer decides when to send data events.

If a producer is faster than a consumer, then we have a much more complicated scenario. We have to slow down a producer. Depends on the nature of the producer, there can be different strategies, for example:



* not generate elements, if the producer is able to control their production rate
* trottle
* block
* buffer elements in a bounded buffer
* drop first or last elements
* cancel the stream if the producer is unable to apply any of the above strategies

In this scenario the reactive stream operates in the ”pull” mode, because the consumer decides when to receive data events.

This model effectively works whether a producer is faster than consumers or vice versa or when this relation can be changed over time.

So the overall model in which reactive streams work can be described as a dynamic “push/pull” model, since a stream switches between “push“ and “pull” models depending on the consumer being able to receive the data events with the rate of the producer or not.


## The Reactive Streams specification

[Reactive Streams](https://www.reactive-streams.org/) is a specification to provide a standard for asynchronous stream processing with non-blocking back pressure for various runtime environments (JVM, .NET and JavaScript) as well as network protocols. Reactive Streams introduces another concurrency model that hides complexity of low-level multi-threading and synchronization and provides a high-level API to:



* process a potentially unbounded number of elements
* in sequence
* asynchronously passing elements between components
* with mandatory non-blocking backpressure

The Reactive Streams specification for the JVM (the latest version 1.0.4 was released at May 26th, 2022) consists of the following parts:



* textual [specification](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.4/README.md#specification)
* the Java [API](https://www.reactive-streams.org/reactive-streams-1.0.4-javadoc) that contains four interfaces that should be implemented according to this specification
* the Technology Compatibility Kit ([TCK](https://www.reactive-streams.org/reactive-streams-tck-1.0.4-javadoc)), a standard test suite for conformance testing of implementations
* [implementation examples](https://www.reactive-streams.org/reactive-streams-examples-1.0.4-javadoc)

Reactive Streams is not a trivial specification. This happened because this specification was created after there were several mature implementations of reactive streams.

Firstly, there is not enough to implement interfaces to make a reactive stream. To work correctly in an asynchronous environment, components of reactive streams must obey its contract. These contracts are summarized as textual specifications and verified in the Technology Compatibility Kit ([TCK](https://www.reactive-streams.org/reactive-streams-tck-1.0.4-javadoc)).

Secondly, the specification doesn’t contain any implementations. It was created to provide a minimal standard that should provide interoperability across already existing implementations of reactive streams. Application developers should rarely implement this specification on their own, but instead use components from existing implementations: Netflix RxJava, Lightbend Akka Streams, Pivotal Reactor, Eclipse Vert.x.

Thirdly, this specification is limited. It covers only mediating the stream of data between components (producers, consumers, processors). Other stream operations (filter, map, reducing, splitting, merging, etc.) are not covered by this specification. An application developer should use this specification to select components from the existing implementations and then use their native APIs.


## The Reactive Streams API

The Reactive Streams API consists of the following interfaces that contains in package _org.reactivestreams_:



* Publisher&lt;T>: A producer of data and control events
* Subscriber&lt;T>: A consumer of events
* Subscription: A connection linking a Publisher and a Subscriber
* Processor&lt;T,R>: A component that acts as both a Subscriber and a Publisher

![Reactive Streams API](/images/Reactive_Streams_API.png)


### Publisher

The [Publisher](https://github.com/reactive-streams/reactive-streams-jvm#1-publisher-code) interface represents a producer of a potentially unbounded number of sequenced data and control events. A Publisher produces events according to the demand received from one or many Subscribers.


```
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```


This interface has the following method:



* The _Publisher#subscribe(Subscriber)_ method requests a Publisher to start sending data to a Subscriber

Publishers may vary about whether Subscribers receive events that were produced before they subscribed. Those producers who can be repeated and do not start until subscribed to, are _cold_ producers (in-memory iterators, file readings, database queries, etc). Those producers who cannot be repeated and start immediately regardless of whether it has subscribers, are _hot_ producers (keyboard and mouse events, sensor events, network requests, etc).


### Subscriber

The [Subscriber](https://github.com/reactive-streams/reactive-streams-jvm#2-subscriber-code) interface represents a consumer of events. Multiple Subscribers can subscribe and unsubscribe to a Producer at various points in time. A Subscriber can only subscribe once to a single Publisher.


```
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);
    public void onNext(T t);
    public void onError(Throwable t);
    public void onComplete();
}
```


This interface has the following methods:



* The _onSubscribe(Subscription)_ method is invoked when the Producer accepted the new Subscription
* The _onNext(T)_ method is invoked on each received item
* The _onError(Throwable)_ method is invoked at failed completion
* The _onComplete()_ method is invoked at successful completion


### Subscription

The [Subscription](https://github.com/reactive-streams/reactive-streams-jvm#3-subscription-code) interface represents a connection between a Publisher and a Subscriber. Through a Subscription, a Subscriber may request items from the Producer or cancel connection at any time. A Subscription can only be used only once and only by the Subscriber that created it.


```
public interface Subscription {
    public void request(long n);
    public void cancel();
}
```


This interface has the following methods:



* The _request(long)_ method adds the given number of items to the unfulfilled demand for this Subscription
* The _cancel()_ method requests the Publisher to stop sending items and clean up resources


### Processor

The [Processor](https://github.com/reactive-streams/reactive-streams-jvm#4processor-code) interface represents a processing stage, which is both a Subscriber and a Publisher and obeys the contracts of both. A Processor works as a Consumer for a previous stage (a Publisher or a previous Processor) and as a Producer for a next stage (a next Processor or a Consumer).


```
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```


The Processor interface is intended to implement intermediate stream operations (filter, map, reduce, split, merge, etc.).


## The Reactive Streams workflow

![Reactive Streams workflow](/images/Reactive_Streams_workflow.png)

When a Subscriber wants to start receiving items from a Publisher, it calls the _Publisher#subscribe(Subscriber)_ method and passes itself as a parameter. If the Publisher accepts the request from the Subscriber, then it creates a new Subscription object and invokes the _Subscriber#onSubscribe(Subscription)_ method. If the Publisher rejects the request of the Subscriber or otherwise fails (for example, already subscribed), it invokes _Subscriber#onError(Throwable)_ method.

After setting the relation between the Publisher and the Subscriber through the Subscription object, the Subscriber can request items and the Publisher can produce them. When the Subscriber wants to receive items, it calls the _Subscription#request(long)_ method with a number of requested items. Typically, the first such call occurs in the _Subscriber#onSubscribe_ method. If the Subscriber wants to stop receiving items, it calls the _Subscription#cancel()_ method. After invoking this method the Subscriber may still receive items to meet previously requested demand.

<sub>A canceled Subscription does not receive <em>Subscriber#onComplete() </em>or <em>Subscriber#onError(Throwable) </em>events.</sub>

The Producer sends each requested item by calling the _Subscriber#onNext(T)_ method only in response to the previous call the _Subscription#request(long)_ method but never by its own. A Publisher can send less than is requested if the stream ends but then must emit either _Subscriber#onError(Throwable)_ or _Subscriber#onComplete()_.

<sub>After invocation of <em>Subscriber#onError(Throwable)</em> or <em>Subscriber#onComplete()</em> no other events will be sent to the Subscriber by the current Subscription.</sub>

When there are no more items, the Publisher completes the Subscription normally by calling the _Subscriber#onCompleted()_ method. When in the Producer happens an unrecoverable exception, the Publisher completes the Subscription exceptionally by calling the _Subscriber#onError(Throwable)_ method.

>To make a reactive stream _push_-based, a consumer can once invoke the _Subscription#request(long)_ method with parameter _Long.MAX_VALUE_. To make a reactive stream _pull_-based, a consumer can invoke the _Subscription#request(long)_ method with parameter _1_ each time it is ready.


## The JDK Flow API

Starting from Java 9, Reactive Streams have become a part of the JDK. In Java 9 was added the class [java.util.concurrent.Flow](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Flow.html) that contains nested interfaces _Publisher_, _Subscriber_, _Subscription_, _Processor_ that are 100% semantically equivalent to their respective Reactive Streams counterparts. The only implementation of the Reactive Streams specification that JDK provides so far is the _java.util.concurrent.SubmissionPublisher_ class that implements the _Publisher_ interface.

>Reactive Streams contains the class _org.reactivestreams.FlowAdapters_ is a bridge between Reactive Streams API in the _org.reactivestreams package_ and the Java 9 _java.util.concurrent.Flow_ API.


### SubmissionPublisher

The [SubmissionPublisher](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/SubmissionPublisher.htm) class is an implementation of the _Publisher_ interface that is compliant with the Reactive Streams specification. The _SubmissionPublisher_ class uses an executor (to send items to multiple consumers asynchronously) and a bounded buffer for each consumer (to mediate rates of a producer and a consumer).

>Normally by default the _SubmissionPublisher_ class uses _ForkJoinPool.commonPool()_ as an executor and _Flow.defaultBufferSize() == 256_ as the maximum buffer capacity.

The Publisher can use different back-pressure strategies:



* _buffering_: for each Subscriber the publisher creates a Subscription that contains a buffer with initially a minimal size that can be extended to the maximum buffer capacity.
* _throttling_: during publishing, the Publisher can use methods _estimateMaximumLag_ (an estimate of the maximum number of items produced but not yet consumed among all current subscribers) and _estimateMinimumDemand_ (an estimate of the minimum number of items requested but not yet produced, among all current subscribers) to predict the buffer saturation and control its publishing rate.
* _blocking_: if the Publisher uses the _submit_ method to send items to the subscribers, then when the buffer becomes full, this method blocks until free space is available in the buffer.
* _dropping_ and _retrying_: if the Publisher uses the _offer_ method to send items to the subscribers, then when the buffer becomes full, this method can drop items (either immediately or with a timeout) with or without the possibility to retry them once.

There are two ways to subscribe consumers to the _SubmissionPublisher_.

The first way is to use the inherited from the _Publisher_ interface method _subscribe(Subscriber&lt;? super T> subscriber)_ that allows the use of the _onSubscribe, onNext, on Error, onComplete_ handlers.


```
try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

   publisher.subscribe(new Flow.Subscriber<>() {

       private Flow.Subscription subscription;

       @Override
       public void onSubscribe(Flow.Subscription subscription) {
           logger.info("subscribed");
           this.subscription = subscription;
           this.subscription.request(1);
       }

       @Override
       public void onNext(Long item) {
           delay();
           logger.info("received: {}", item);
           this.subscription.request(1);
       }

       @Override
       public void onError(Throwable throwable) {
           logger.error("error", throwable);
       }

       @Override
       public void onComplete() {
           logger.info("completed");
       }
   });

   LongStream.range(0, 10).forEach(item -> {
       logger.info("produced: " + item);
       publisher.submit(item);
   });

   publisher.close();

   ExecutorService executorService = (ExecutorService) publisher.getExecutor();
   executorService.awaitTermination(10, TimeUnit.SECONDS);
```


The second way is to use the method _consume(Consumer&lt;? super T> consumer)_ that returns CompletableFuture&lt;Void>. During the subscription, the consumer requests _Long.MAX_VALUE_ items and processes the onNext handler in the _consumer.accept_ method.The _CompletableFuture_ response allows to identify whether the stream is completed normally or exceptionally by the producer or to cancel the stream by the consumer.


```
try (SubmissionPublisher<Long> publisher = new SubmissionPublisher<>()) {

   CompletableFuture<Void> consumerFuture = publisher.consume(item -> {
       delay();
       logger.info("consumed: " + item);
   });

   LongStream.range(0, 10).forEach(item -> {
       logger.info("produced: " + item);
       publisher.submit(item);
   });

   publisher.close();

   consumerFuture.get();
}
