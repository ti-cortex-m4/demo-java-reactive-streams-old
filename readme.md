# Reactive Streams specification in Java


## Introduction

Reactive Streams is a cross-platform specification for processing a potentially unlimited sequence of events across asynchronous boundaries (threads, processes, or network-connected computers) with non-blocking backpressure. A reactive stream contains a publisher, which sends forward _data_, _error_, _completion_ events, and subscribers, which send backward _request_ and _cancel_ backpressure events. There can also be intermediate processors between the publisher and the subscribers that filter or modify events.

<sub>Backpressure is application-level flow control from the subscriber to the publisher to control the sending rate.</sub>

The Reactive Streams specification is designed to efficiently process (in terms of CPU and memory usage) time-ordered sequences of events. For efficient CPU usage, the specification describes the rules for asynchronous and non-blocking events processing in various stages (producers, processors, consumers). For efficient memory usage, the specification describes the rules for switching between _push_ and _pull_ communication models based on the events processing rate, which avoids using unbounded buffers.


## Problems and solutions

When designing systems for transferring items from a producer to a consumer, the goal is to send them with minimal latency and maximum throughput.

<sub>Latency is the time between sending an item from the producer and its receiving by the consumer. Throughput is the number of items sent from producer to consumer per unit of time.</sub>

However, the producer and the consumer may have limitations that can prevent the system from achieving the best performance:



* The consumer can be slower than the producer.
* The consumer may not be able to skip items that it does not have time to process.
* The producer may not be able to slow or stop sending items that the consumer does not have time to process.
* The producer and consumer may have a limited amount of CPU cores to process items asynchronously and memory to buffer items.
* The communication channel between the producer and the consumer may have limited bandwidth.

There are several patterns for sequential item processing that solve some or most of the above limitations:



* Iterator
* Observer
* Reactive Extensions
* Reactive Streams

These patterns are divided into two groups: synchronous _pull_ communication models (in which the consumer determines when to receive items from the producer) and asynchronous _push_ communication models (in which the producer determines when to send items to the consumer).


### Iterator

In the Iterator pattern, the consumer synchronously _pulls_ items from the producer one by one. The producer sends an item only when the consumer requests it. If the producer has no item at the time of the request, it sends an empty response.

![Iterator](/images/Iterator.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer cannot request the next item if he has not yet processed the previous one.
* The consumer can stop the exchange at any time.

Cons:



* The latency may not be optimal due to an incorrectly chosen pulling period (too long pulling period leads to high latency; too short pulling period wastes CPU and I/O resources).
* The throughput is not optimal because it takes one request-response to send each item.
* The consumer cannot determine if the producer has finished generating items.

When using the Iterator pattern, which transfers items one at a time, latency and throughput are often unsatisfactory. To improve these parameters with minimal changes, the same Iterator pattern can transfer items in batches instead of one at a time.

![Iterator with batching](/images/Iterator_with_batching.png)

Pros:



* Throughput increases as the number of requests/responses decreases from one for each item to one for all items in a batch.

Cons:



* The latency increases because the producer needs more time to send more items.
* If the batch size is too large, it may not fit in the memory of the producer or the consumer.
* If the consumer wants to stop processing, he can do so no sooner than he receives the entire batch.


### Observer

In the Observer pattern, one or many consumers subscribe to the producer's events. The producer asynchronously _pushes_ events to all subscribed consumers as soon as they are generated. The consumer can unsubscribe from the producer if it does not need further events.

![Observer](/images/Observer.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The latency is lower than in synchronous _pull_ communication models because the producer sends events to the consumer as soon as they become available.

Cons:



* A slower consumer may be overwhelmed by events from a faster producer.
* The consumer cannot determine when the producer has finished generating items.
* Implementing concurrent producers and consumers may be non-trivial.


### Reactive Extensions

Reactive Extensions (ReactiveX) is a family of multi-platform frameworks for handling synchronous or asynchronous event streams, originally created by Erik Meijer at Microsoft. The implementation of Reactive Extensions for Java is the Netflix RxJava framework.

In a simplified way, Reactive Extensions are a combination of the Observer and Iterator patterns and functional programming. From the Observer pattern, they took the consumer’s ability to subscribe to the producer’s events. From the Iterator pattern, they took the ability to handle event streams of three types (data, error, completion). From functional programming, they took the ability to handle event streams with chained methods (transform, filter, combine, etc.).

![Reactive Extensions](/images/Reactive_Extensions.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The consumer can determine when the producer has finished generating events.
* The latency is lower than in synchronous _pull_ communication models because the producer sends events to the consumer as soon as they become available.
* The consumer can uniformly handle event streams of three types (data, error, completion).
* Handling event streams with chained methods can be easier than with nested event handlers.

Cons:



* A slower consumer may be overwhelmed by events from a faster producer.
* Implementing concurrent producers and consumers may be non-trivial.


### Reactive Streams

Reactive Streams are a further development of Reactive Extensions, which use backpressure to match producer and consumer performance. In a simplified way, Reactive Streams are a combination of Reactive Extensions and batching.

The main difference between them is who is the initiator of the exchange. In Reactive Extensions, a publisher sends events to a subscriber as soon as they become available and in any number. In Reactive Streams, a publisher must send events to a subscriber only after they have been requested and no more than the requested number.

![Reactive Streams](/images/Reactive_Streams.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The consumer can determine when the producer has finished generating events.
* The latency is lower than in synchronous _pull_ communication models because the producer sends events to the consumer as soon as they become available.
* The consumer can uniformly handle event streams of three types (data, error, completion).
* Handling event streams with chained methods can be easier than with nested event handlers.
* The consumer can request events from the producer depending on the need.

Cons:



* Implementing concurrent producers and consumers may be non-trivial.


## Backpressure

There are several solutions for the problem where a producer generates events faster than a consumer processes them. This does not happen in _pull_ communication models because the consumer is the initiator of the exchange. In _push_ communication models, the producer cannot usually determine the sending rate, so the consumer may eventually receive more events than it can process. Backpressure is a solution to this problem by informing the producer about the processing rate of its consumers.

Without the use of backpressure, the consumer has a few solutions to deal with excessive events:



* buffer events
* drop events
* drop events and request the producer to resend them by their identifiers

<sub>Any solution that includes dropping events on the consumer may be inefficient because these events still require I/O operations to send them from the producer.</sub>

The backpressure in reactive streams is implemented as follows. To start receiving events from the producer, the consumer _pulls_ the number of items it wants to receive. Only then does the producer _push_ events to the consumer; the producer never sends them on its own initiative. After the consumer has processed all the requested events, this cycle is repeated. In a particular case, if the consumer is known to be faster than the producer, it can work in a _push_ communication model and request all items immediately after subscribing. Or vice versa, if the consumer is known to be slower than the producer, it can work in a _pull_ communication model and request the next items only after the previous ones have been processed. Thus, the model in which reactive streams operate can be described as a _dynamic push/pull_ communication model. It works effectively if the producer is faster or slower than the consumer, or even when that ratio can change over time.

With the use of backpressure, the producer has much more solutions to deal with excessive events:



* buffer events
* drop events
* pause generation events
* block the producer
* cancel the event stream

Which solutions to use for a particular reactive stream depends on the nature of the events. But backpressure is not a _silver bullet_. It simply shifts the problem of performance mismatch to the producer's side, where it is supposed to be easier to solve. However, in some cases, there are better solutions than using backpressure, such as simply dropping excessive events on the consumer's side.


## The Reactive Streams specification

Reactive Streams is a [specification](https://www.reactive-streams.org/) to provide a standard for asynchronous stream processing with non-blocking backpressure for various runtime environments (JVM, .NET, and JavaScript) and network protocols. The Reactive Streams specification was created by engineers from Kaazing, Lightbend, Netflix, Pivotal, Red Hat, Twitter, and others.

The specification describes the concept of a _reactive stream_ that has the following features:



* reactive streams are potentially _unbounded_: they can handle zero, one, many, or an infinite number of events.
* reactive streams are _sequential_: a consumer processes events in the same order that a producer sends them.
* reactive streams can be _synchronous_ or _asynchronous_: they can use computing resources (CPU cores) for parallel processing in separate stream stages.
* reactive streams are _non-blocking_: they do not waste computing resources if the performance of a producer and a consumer is different.
* reactive streams use _mandatory backpressure_: a consumer can request events from a producer according to their processing rate.
* reactive streams use _bounded buffers_: they can be implemented without unbounded buffers, avoiding memory out-of-memory errors.

The Reactive Streams [specification for the JVM](https://github.com/reactive-streams/reactive-streams-jvm) (the latest version 1.0.4 was released on May 26th, 2022) contains the textual specification and the Java API, which contains four interfaces that must be implemented according to this specification. It also includes the Technology Compatibility Kit (TCK), a standard test suite for conformance testing of implementations.

Importantly, the Reactive Streams specification was created after several mature but incompatible implementations of Reactive Streams already existed. Therefore, the specification is currently limited and contains only low-level APIs. Application developers should use this specification to provide _interoperability_ between existing implementations. To have high-level functional APIs (transform, filter, combine, etc.), application developers should use implementations of this specification (Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, etc.) through their native APIs.


## The Reactive Streams API

The Reactive Streams API consists of the four interfaces, which are located in the _org.reactivestreams_ package:



* The Publisher&lt;T> interface represents a producer of data and control events.
* The Subscriber&lt;T> interface represents a consumer of events.
* The Subscription interface represents a connection between a Publisher and a Subscriber.
* The Processor&lt;T,R> interface represents a processor of events that acts as both a Subscriber and a Publisher.

![Reactive Streams API](/images/Reactive_Streams_API.png)


### Publisher

The Publisher interface represents a producer of potentially unlimited sequenced data and control events. A Publisher produces events according to the demand received from one or many Subscribers.

<sub>Demand is the aggregated number of items requested by a Subscriber which is yet to be delivered by the Publisher.</sub>


```
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```


This interface has the following method:



* The _subscribe(Subscriber)_ method requests the Publisher to start sending events to a Subscriber.


### Subscriber

The Subscriber interface represents a consumer of events. Multiple Subscribers can subscribe to and unsubscribe from a Producer at different times.


```
public interface Subscriber<T> {
    public void onSubscribe(Subscription s);
    public void onNext(T item);
    public void onError(Throwable t);
    public void onComplete();
}
```


This interface has the following methods:



* The _onSubscribe(Subscription)_ method is invoked when the Producer accepts a new Subscription.
* The _onNext(T)_ method is invoked on each received item.
* The _onError(Throwable)_ method is invoked on erroneous completion.
* The _onComplete()_ method is invoked on successful completion.


### Subscription

The Subscription interface represents a connection between a Publisher and a Subscriber. Through a Subscription, the Subscriber can request items from the Publisher or cancel the connection.


```
public interface Subscription {
    public void request(long n);
    public void cancel();
}
```


This interface has the following methods:



* The _request(long)_ method adds the given number of items to the unfulfilled demand for this Subscription.
* The _cancel()_ method requests the Publisher to _eventually_ stop sending items.


### Processor

The Processor interface represents a processing stage that extends the Subscriber and Publisher interfaces and obeys the contracts of both.. It acts as a Subscriber for the previous stage of a reactive stream and as a publisher for the next one.


```
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```



## The Reactive Streams workflow

When a Subscriber wants to start receiving events from a Publisher, it calls the _Publisher.subscribe(Subscriber)_ method and passes itself as a parameter. If the Publisher accepts the request, it creates a new Subscription instance and invokes the _Subscriber.onSubscribe(Subscription)_ method with it. If the Publisher rejects the request or otherwise fails, it invokes the _Subscriber.onError(Throwable)_ method.

Once the connection between Publisher and Subscriber is established through the Subscription object, the Subscriber can request events and the Publisher can send them. When the Subscriber wants to receive events, it calls the _Subscription#request(long)_ method with the number of items requested. Typically, the first such call occurs in the _Subscriber.onSubscribe_ method.

If the Subscriber wants to stop receiving events, it calls the _Subscription.cancel()_ method. After this method is called, the Subscriber can continue to receive events to meet the previously requested demand. A canceled Subscription does not receive _Subscriber.onComplete()_ or _Subscriber.onError(Throwable)_ events.

The Publisher sends each requested event by calling the _Subscriber.onNext(T)_ method only in response to a previous request by the _Subscription.request(long)_ method, but never by itself. A Publisher can send fewer events than requested if the stream ends, but then must call either the _Subscriber.onError(Throwable)_ or _Subscriber.onComplete()_ methods. After invocation of _Subscriber.onError(Throwable)_ or _Subscriber.onComplete()_ events, the current Subscription will not send any other events to the Subscriber.

When there are no more events, the Publisher completes the Subscription normally by calling the _Subscriber.onCompleted()_ method. When an unrecoverable exception occurs in the Publisher, it completes the Subscription exceptionally by calling the _Subscriber.onError(Throwable)_ method.
