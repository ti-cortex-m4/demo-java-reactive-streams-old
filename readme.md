<!-- Output copied to clipboard! -->

<!-----

Yay, no errors, warnings, or alerts!

Conversion time: 2.167 seconds.


Using this Markdown file:

1. Paste this output into your source file.
2. See the notes and action items below regarding this conversion run.
3. Check the rendered output (headings, lists, code blocks, tables) for proper
   formatting and use a linkchecker before you publish this page.

Conversion notes:

* Docs to Markdown version 1.0β34
* Thu May 04 2023 06:18:31 GMT-0700 (PDT)
* Source doc: #2
----->



# Reactive Streams specification in Java


## Introduction

_Reactive Streams_ is a cross-platform specification for processing a possibly unbounded sequence of events across asynchronous boundaries (threads, actors, processes, or network-connected computers) with non-blocking backpressure. A reactive stream contains a publisher, which sends forward _data_, _error_, _completion_ events, and subscribers, which send backward _request_ and _cancel_ backpressure events. There may be intermediate event processors between the publisher and the subscribers.

<sub>Backpressure is application-level flow control from the subscriber to the publisher to control the emission rate.</sub>

The Reactive Streams specification was created to solve two problems. First, to create a solution for processing time-ordered sequences of events with automatic switching between _push_ and _pull_ models based on the consumption rate and without unbounded buffering. Second, to create an interoperable solution that can be used in different frameworks, environments, and networks.

The Reactive Streams specification is designed for the various programming platforms (.NET, JVM, JavaScript) and network protocols. This specification is implemented, in particular:



* general-purpose frameworks (Eclipse Vert.x, Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, Parallel Universe Quasar, SmallRye Mutiny)
* web frameworks (Lightbend Play Framework, Oracle Helidon, Pivotal WebFlux, Ratpack)
* relational and non-relational databases (Apache Cassandra, Elasticsearch, MongoDB, PostgreSQL)
* message brokers (Apache Kafka, Pivotal RabbitMQ)
* cloud providers (AWS SDK for Java 2.0)
* network protocols (RSocket)


## Protocol evolution

When transmitting items from the producer to the consumer, the goal is to send items with minimal latency and maximum throughput.

<sub>Latency is the time between the generation of an item at the producer and its arrival at the consumer. Throughput is the number of items sent from producer to consumer per unit of time.</sub>

However, the producer and the consumer may have limitations that can prevent the best performance from being achieved:



* The consumer can be slower than the producer.
* The producer may not be able to slow or stop sending items that the consumer does not have time to process.
* The consumer may not be able to skip items that it does not have time to process.
* The producer and consumer may have a limited amount of memory to buffer items and CPU cores to process items asynchronously.
* The communication channel between the producer and the consumer may have limited bandwidth.

There are several patterns for sequential item processing that allow bypassing some or most of the above limitations:



* Iterator
* Observer
* Reactive Extensions
* Reactive Streams

These patterns fall into two groups: synchronous _pull_ models (in which the consumer determines when to request items from the producer) and asynchronous _push_ models (in which the producer determines when to send items to the consumer).


### Iterator

In the Iterator pattern, the consumer synchronously _pulls_ items from the producer one by one. The producer sends an item only when the consumer requests it. If the producer has no item at the time of the request, it sends an empty response.

![Iterator](/images/Iterator.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer cannot request the next item if he has not yet processed the previous one.
* The consumer can stop the exchange at any time.

Cons:



* The latency may not be optimal due to an incorrectly chosen pulling period (too long pulling period leads to high latency, too short pulling period wastes CPU and I/O resources).
* The throughput is not optimal because it takes one request-response to send each item.
* The consumer cannot determine if the producer is done sending items.

When using the Iterator pattern, which transmits items one at a time, latency and throughput are often unsatisfactory. To improve these parameters with minimal changes, the same Iterator pattern can transmit items in batches instead of one at a time.

![Iterator with batching](/images/Iterator_with_batching.png)

Pros:



* Throughput increases as the number of requests and responses decreases from one for _each_ item to one for _all_ items in a batch.

Cons:



* The latency increases because the producer needs more time to send more items.
* If the batch size is too large, it may not fit in the memory of the producer or the consumer.
* If the consumer wants to stop processing, he can do so no sooner than he receives the entire item batch.


### Observer

In the Observer pattern, one or many consumers subscribe to the producer's events. The producer asynchronously _pushes_ events to all subscribed consumers as soon as they become available. The consumer can unsubscribe from the producer if it does not need further events.

![Observer](/images/Observer.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The latency is lower than in synchronous _pull_ models because the producer sends events to the consumer as soon as they become available.

Cons:



* A slower consumer may be overwhelmed by the stream of events from a faster producer.
* The consumer cannot determine if the producer has finished sending events.
* Implementing a concurrent producer may be non-trivial.


### Reactive Extensions

Reactive Extensions (ReactiveX) is a family of multi-platform frameworks for handling synchronous or asynchronous event streams, originally created by Erik Meijer at Microsoft.

<sub>The implementation of Reactive Extensions for Java is the Netflix RxJava framework.</sub>

In a simplified way, Reactive Extensions are a combination of the Observer and Iterator patterns and functional programming. From the Observer pattern, they took the ability of the consumer to subscribe to producer events. From the Iterator pattern, they took the ability to handle event streams of three types (data, error, completion). From functional programming, they took the ability to handle event streams with chained methods (transform, filter, combine, etc.).

![Reactive Extensions](/images/Reactive_Extensions.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The consumer can determine when the producer has finished sending events.
* The latency is lower than in synchronous _pull_ models because the producer sends events to the consumer as soon as they become available.
* The consumer can uniformly handle the stream of events of the three types (data, error, completion).
* Handling event streams with chained methods can be easier than with nested event handlers.

Cons:



* A slower consumer may be overwhelmed by the stream of events from a faster producer.
* Implementing a concurrent producer may be non-trivial.


### Reactive Streams

Reactive Streams are a further development of Reactive Extensions, which were created to solve the problem of unbounded buffers that were used to reconcile the processing rates of a faster producer and a slower consumer. In a simplified way, Reactive Streams are a combination of Reactive Extensions and batching.

In Reactive Extensions, a Publisher can send events to a Subscriber as soon as they become available and in any quantity. In Reactive Streams, a Publisher must send events to a Subscriber only after requesting them and no more than the requested quantity.

![Reactive Streams](/images/Reactive_Streams.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The consumer can determine when the producer has finished sending the events.
* The latency is lower than in synchronous _pull_ models because the producer sends events to the consumer as soon as they become available.
* The consumer can uniformly handle the stream of events of the three types (data, error, completion).
* Handling event streams with chained methods can be easier than handling them with nested event handlers.
* The consumer can request events from the producer depending on its demand.

Cons:



* Implementing a concurrent producer may be non-trivial.


## Backpressure

There are several solutions for situations where the consumer processes events slower than the producer sends them. This is not a problem for _pull_ models because the consumer is the initiator of the exchange. In _push_ models, the producer usually has no way to determine the rate of sending events, so the consumer may receive more events than it can handle. This performance mismatch can be resolved by backpressure on the consumer or the producer.

There are several ways to deal with the backpressure on the consumer side:



* buffer events in a bounded buffer
* drop events
* drop events _and_ request the producer to resend them by their identifiers

In this situation, the choice is either to lose the events or to introduce additional I/O operations to resend them.

<sub>Backpressure on the consumer side may be inefficient because dropped events require I/O operations to send them from the producer.</sub>

Backpressure is a solution in reactive streams to the problem of informing the producer about the processing rate of its consumers. To start sending events from the producer, the consumer _pulls_ the number of events it wants to receive. Only then does the producer send events to the consumer, the producer never sends events on its own initiative. If the consumer is faster than the producer, he can work in the _push_ model and request all events immediately after subscribing. If the consumer is slower than the producer, he can work in the _pull_ model and request the next events only after the previous ones have been processed. So, the model in which reactive streams operate can be described as a _dynamic push/pull_ model. This model works effectively if the producer is faster or slower than the consumer, or even when that ratio can change over time.

There are several ways to deal with the backpressure on the producer:



* pause sending events
* buffer events in a bounded buffer
* block the producer
* drop events
* cancel the event stream

Backpressure shifts the overflow problem to the producer side, where it is supposed to be easier to solve. However, depending on the nature of the events, there may be better solutions than backpressure, such as simply dropping the events.


## The Reactive Streams specification

Reactive Streams is a [specification](https://www.reactive-streams.org/) to provide a standard for asynchronous stream processing with non-blocking backpressure for various runtime environments (JVM, .NET, and JavaScript) and network protocols. The Reactive Streams specification was created by engineers from Kaazing, Lightbend, Netflix, Pivotal, Red Hat, Twitter, and others.

The specification describes the concept of a _reactive stream_ that has the following features:



* Reactive Streams are potentially _unbounded_: they can handle zero, one, many, or an infinite number of events.
* Reactive Streams are _sequenced_: consumers process events in the same order in which the producer sends them.
* Reactive Streams can be _synchronous_ or _asynchronous_: they can use computing resources (CPU cores for one computer) for parallel processing in separate stream components.
* Reactive Streams are _non-blocking_: they do not waste computing resources if the producer and consumer rates are different.
* Reactive Streams use _mandatory backpressure_: consumers can request events from the producer according to their consumption rate.
* Reactive Streams use _bounded buffers_: they can be implemented without unbounded buffers, which can lead to out-of-memory errors.

The Reactive Streams [specification for the JVM](https://github.com/reactive-streams/reactive-streams-jvm) (the latest version 1.0.4 was released on May 26th, 2022) contains the textual specification and the Java API, which contains four interfaces that must be implemented according to this specification. They also include the Technology Compatibility Kit (TCK), a standard test suite for conformance testing of implementations.

The Reactive Streams specification was created after several mature but incompatible implementations of Reactive Streams already existed. Therefore, the specification is currently limited and contains only low-level APIs. Application developers should use this specification to provide _interoperability_ between existing implementations. To have high-level functional APIs (transform, filter, combine, etc.), application developers should use implementations of this specification (Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, etc.) through their native APIs.


## The Reactive Streams API

The Reactive Streams API consists of the following interfaces, which are located in the _org.reactivestreams_ package:



* The Publisher&lt;T> interface represents a producer of data and control events.
* The Subscriber&lt;T> interface represents a consumer of events.
* The Subscription interface represents a connection that links a Publisher and a Subscriber.
* The Processor&lt;T,R> interface represents a processor of events that acts as both a Subscriber and a Publisher.

![Reactive Streams API](/images/Reactive_Streams_API.png)


### Publisher

The Publisher interface represents a producer of a potentially unbounded number of sequenced data and control events. A Publisher produces events according to the demand received from one or many Subscribers.

<sub>Demand is the aggregated number of items requested by a Subscriber which is yet to be fulfilled by the Publisher.</sub>


```
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```


This interface has the following method:



* The _Publisher.subscribe(Subscriber)_ method requests the Publisher to start sending events to the Subscriber.


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
* The _onError(Throwable)_ method is invoked at erroneous completion.
* The _onComplete()_ method is invoked on successful completion.

<sub>A Publisher must invoke Subscriber methods <em>onSubscribe</em>, <em>onNext</em>, <em>onError</em>, <em>onComplete</em> serially, which means that the invocations must not overlap. When the invocations are performed asynchronously, the caller has to establish a <em>happens-before</em> relationship between them.</sub>


### Subscription

The Subscription interface represents a connection between a Publisher and a Subscriber. Through a Subscription, the Subscriber can request items from the Publisher or cancel the connection at any time.


```
public interface Subscription {
    public void request(long n);
    public void cancel();
}
```


This interface has the following methods:



* The _request(long)_ method adds the given number of items to the unfulfilled demand for this Subscription.
* The _cancel()_ method requests the Publisher to stop sending items and clean up resources.

<sub>A Subscriber must invoke a Subscription’s methods <em>request</em>, <em>cancel</em> serially, which means that the invocations must not overlap. When the invocations are performed asynchronously, the caller has to establish a <em>happens-before</em> relationship between them.</sub>


### Processor

The Processor interface represents a processing stage, which is both a Subscriber and a Publisher, and is subject to the contracts of both. The Processor interface is designed to implement intermediate stream operations (transform, filter, combine, etc.).


```
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```



## The Reactive Streams workflow

When a Subscriber wants to start receiving events from a Publisher, it calls the _Publisher.subscribe(Subscriber)_ method and passes itself as a parameter. If the Publisher accepts the request, it creates a new Subscription instance and invokes the _Subscriber.onSubscribe(Subscription)_ method with it. If the Publisher rejects the request or otherwise fails (for example, it has already subscribed), it invokes the _Subscriber.onError(Throwable)_ method.

Once the connection between Publisher and Subscriber is established through the Subscription object, the Subscriber can request events and the Publisher can send them. When the Subscriber wants to receive events, it calls the _Subscription#request(long)_ method with the number of items requested. Typically, the first such call occurs in the _Subscriber.onSubscribe_ method. If the Subscriber wants to stop receiving events, it calls the _Subscription#cancel()_ method. After this method is called, the Subscriber can continue to receive events to meet the previously requested demand. A canceled Subscription does not receive _Subscriber.onComplete()_ or _Subscriber.onError(Throwable)_ events.

The Publisher sends each requested event by calling the _Subscriber.onNext(T)_ method only in response to a previous request by the _Subscription#request(long)_ method, but never by itself. A Publisher can send fewer events than requested if the stream ends, but then must call either the _Subscriber.onError(Throwable)_ or _Subscriber.onComplete()_ methods. After invocation of _Subscriber.onError(Throwable)_ or _Subscriber.onComplete()_ events, the current Subscription will not send any other events to the Subscriber.

When there are no more events, the Publisher completes the Subscription normally by calling the _Subscriber.onCompleted()_ method. When an unrecoverable exception occurs in the Publisher, it completes the Subscription exceptionally by calling the _Subscriber.onError(Throwable)_ method.

<sub>To make a reactive stream <em>push</em>-based, a Subsriber can call the <em>Subscription#request(long)</em> method once with the parameter <em>Long.MAX_VALUE</em>. To make a reactive stream <em>pull</em>-based, a Subsriber can call the <em>Subscription#request(long)</em> method with parameter <em>1</em> every time it is ready to handle the next event.</sub>


## The JDK Flow API

Reactive Streams started to be supported in JDK 9 in the form of the Flow API. The _java.util.concurrent.Flow_ class contains nested static interfaces Publisher, Subscriber, Subscription, Processor, which are 100% semantically equivalent to their respective Reactive Streams counterparts. The Reactive Streams specification contains the _org.reactivestreams.FlowAdapters_ class, which is a bridge between the Reactive Streams API in the _org.reactivestreams_ package and the JDK Flow API in the _java.util.concurrent.Flow_ class. The only implementation of the Reactive Streams specification that JDK provides so far is the _java.util.concurrent.SubmissionPublisher_ class that implements the Publisher interface.


## Code examples


### Synchronous Publisher

The following code example demonstrates a synchronous Publisher that sends a finite sequence of events from an Iterator. The _synchronous_ Publisher processes its _subscribe_ method and the Subscriptions’ _request_ and _cancel_ methods in the caller’s thread. This Publisher is _multicast_ and can send items to multiple Subscribers, storing information about each connection in a private implementation of the Subscription interface. This includes the current Iterator instance, the demand (the aggregated number of items requested by a Subscriber which is yet to be fulfilled by the Publisher), and the connection cancellation flag. To make a _cold_ Publisher that sends the same sequence of events for each Subscriber, the Publisher stores a Supplier that must return a new Iterator instance for each new Subscription. The Publisher uses different types of error handling (throwing an exception or calling the onError handler) according to the Reactive Streams specification.

<sub>The GitHub repository has unit tests to verify that this Publisher complies with all the specification rules that are checked in its TCK.</sub>


```java
public class SyncIteratorPublisher<T> implements Flow.Publisher<T> {

   private final Supplier<Iterator<? extends T>> iteratorSupplier;

   public SyncIteratorPublisher(Supplier<Iterator<? extends T>> iteratorSupplier) {
       this.iteratorSupplier = Objects.requireNonNull(iteratorSupplier);
   }

   @Override
   public void subscribe(Flow.Subscriber<? super T> subscriber) {
       // By rule 1.11, a Publisher may support multiple Subscribers and decide whether each Subscription is unicast or multicast.
       new SubscriptionImpl(subscriber);
   }

   private class SubscriptionImpl implements Flow.Subscription {

       private final Flow.Subscriber<? super T> subscriber;
       private final Iterator<? extends T> iterator;
       private final AtomicLong demand = new AtomicLong(0);
       private final AtomicBoolean cancelled = new AtomicBoolean(false);

       SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
           // By rule 1.9, calling Publisher.subscribe(Subscriber) must throw a NullPointerException when the given parameter is null.
           this.subscriber = Objects.requireNonNull(subscriber);

           Iterator<? extends T> iterator = null;
           try {
               iterator = iteratorSupplier.get();
           } catch (Throwable t) {
               // By rule 1.9, a Publisher must call onSubscribe prior onError if Publisher.subscribe(Subscriber) fails.
               subscriber.onSubscribe(new Flow.Subscription() {
                   @Override
                   public void cancel() {
                   }

                   @Override
                   public void request(long n) {
                   }
               });
               // By rule 1.4, if a Publisher fails it must signal an onError.
               doError(t);
           }
           this.iterator = iterator;

           if (!cancelled.get()) {
               subscriber.onSubscribe(this);
           }
       }

       @Override
       public void request(long n) {
           logger.info("subscription.request: {}", n);

           // By rule 3.9, while the Subscription is not cancelled, Subscription.request(long) must signal onError with a IllegalArgumentException if the argument is <= 0.
           if ((n <= 0) && !cancelled.get()) {
               doCancel();
               subscriber.onError(new IllegalArgumentException("non-positive subscription request"));
               return;
           }

           for (;;) {
               long oldDemand = demand.get();
               if (oldDemand == Long.MAX_VALUE) {
                   // By rule 3.17, a demand equal or greater than Long.MAX_VALUE may be considered by the Publisher as "effectively unbounded".
                   return;
               }

               // By rule 3.8, while the Subscription is not cancelled, Subscription.request(long) must register the given number of additional elements to be produced to the respective Subscriber.
               long newDemand = oldDemand + n;
               if (newDemand < 0) {
                   // By rule 3.17, a Subscription must support a demand up to Long.MAX_VALUE.
                   newDemand = Long.MAX_VALUE;
               }

               // By rule 3.3, Subscription.request must place an upper bound on possible synchronous recursion between Publisher and Subscriber.
               if (demand.compareAndSet(oldDemand, newDemand)) {
                   if (oldDemand > 0) {
                       return;
                   }
                   break;
               }
           }

           // By rule 1.2, a Publisher may signal fewer onNext than requested and terminate the Subscription by calling onError.
           for (; demand.get() > 0 && iterator.hasNext() && !cancelled.get(); demand.decrementAndGet()) {
               try {
                   subscriber.onNext(iterator.next());
               } catch (Throwable t) {
                   if (!cancelled.get()) {
                       // By rule 1.6, if a Publisher signals onError on a Subscriber, that Subscriber's Subscription must be considered cancelled.
                       doCancel();
                       // By rule 1.4, if a Publisher fails it must signal an onError.
                       subscriber.onError(t);
                   }
               }
           }

           // By rule 1.2, a Publisher may signal fewer onNext than requested and terminate the Subscription by calling onComplete.
           if (!iterator.hasNext() && !cancelled.get()) {
               // By rule 1.6, if a Publisher signals onComplete on a Subscriber, that Subscriber's Subscription must be considered cancelled.
               doCancel();
               // By rule 1.5, if a Publisher terminates successfully it must signal an onComplete.
               subscriber.onComplete();
           }
       }

       @Override
       public void cancel() {
           logger.info("subscription.cancel");
           doCancel();
       }

       private void doCancel() {
           logger.info("subscription.cancelled");
           cancelled.set(true);
       }

       private void doError(Throwable t) {
           // By rule 1.6, if a Publisher signals onError on a Subscriber, that Subscriber's Subscription must be considered cancelled.
           cancelled.set(true);
           subscriber.onError(t);
       }
   }
}
```



### Synchronous Subscriber

The following code example demonstrates a synchronous Subscriber that _pulls_ items one by one. The _synchronous_ Subscriber processes its _onSubscribe_, _onNext_, _onError_, _onComplete_ methods in the Publisher’s thread. The Subscriber also stores its Subscription (to perform backpressure) and its cancellation flag. The Subscriber also uses different types of error handling (throwing an exception or unsubscribing) according to the Reactive Streams specification.

<sub>The GitHub repository has <em>blackbox</em> and <em>whitebox</em> unit tests to verify that this Subscriber complies with all the specification rules that are checked in its TCK.</sub>


```java
public class SyncSubscriber<T> implements Flow.Subscriber<T> {

   private final int id;
   private final AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();
   private final AtomicBoolean cancelled = new AtomicBoolean(false);
   private final CountDownLatch completed = new CountDownLatch(1);

   public SyncSubscriber(int id) {
       this.id = id;
   }

   @Override
   public void onSubscribe(Flow.Subscription subscription) {
       logger.info("({}) subscriber.subscribe: {}", id, subscription);
       // By rule 2.13, calling onSubscribe must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(subscription);

       if (this.subscription.get() != null) {
           // By rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription after an onSubscribe signal if it already has an active Subscription.
           subscription.cancel();
       } else {
           this.subscription.set(subscription);
           // By rule 2.1, a Subscriber must signal demand via Subscription.request(long) to receive onNext signals.
           this.subscription.get().request(1);
       }
   }

   @Override
   public void onNext(T item) {
       logger.info("({}) subscriber.next: {}", id, item);
       // By rule 2.13, calling onNext must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(item);

       // By rule 2.8, a Subscriber must be prepared to receive one or more onNext signals after having called Subscription.cancel()
       if (!cancelled.get()) {
           if (whenNext(item)) {
               // By rule 2.1, a Subscriber must signal demand via Subscription.request(long) to receive onNext signals.
               subscription.get().request(1);
           } else {
               // By rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
               doCancel();
           }
       }
   }

   @Override
   public void onError(Throwable t) {
       logger.error("({}) subscriber.error", id, t);
       // By rule 2.13, calling onError must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(t);

       // By rule 2.4, Subscriber.onError(Throwable) must consider the Subscription cancelled after having received the signal.
       cancelled.set(true);
       whenError(t);
   }

   @Override
   public void onComplete() {
       logger.info("({}) subscriber.complete", id);

       // By rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
       cancelled.set(true);
       whenComplete();
   }

   public void awaitCompletion() throws InterruptedException {
       completed.await();
   }

   // This method is invoked when OnNext signals arrive and returns whether more elements are desired or not (is intended to override).
   protected boolean whenNext(T item) {
       return true;
   }

   // This method is invoked when an OnError signal arrives (is intended to override).
   protected void whenError(Throwable t) {
   }

   // This method is invoked when an OnComplete signal arrives (is intended to override).
   protected void whenComplete() {
       completed.countDown();
   }

   private void doCancel() {
       cancelled.set(true);
       subscription.get().cancel();
   }
}
```



### Synchronous reactive stream

The following code example demonstrates that the _multicast_ synchronous Publisher sends the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to two synchronous Subscribers.


```java
List<String> words = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(() -> List.copyOf(words).iterator());

SyncSubscriber<String> subscriber1 = new SyncSubscriber<>(1);
publisher.subscribe(subscriber1);

SyncSubscriber<String> subscriber2 = new SyncSubscriber<>(2);
publisher.subscribe(subscriber2);

subscriber1.awaitCompletion();
subscriber2.awaitCompletion();
```


The following log demonstrates that the synchronous Publisher sends the sequence of events in the caller's thread, and the synchronous Subscribers receive the sequence of events in the Publisher's thread (the same caller's thread) _one at a time_.


```
11:32:37.310  main             (1) subscriber.subscribe: SyncIteratorPublisher$SubscriptionImpl@1f28c152
11:32:37.313  main             subscription.request: 1
11:32:37.313  main             (1) subscriber.next: The
11:32:37.313  main             subscription.request: 1
11:32:37.313  main             (1) subscriber.next: quick
11:32:37.313  main             subscription.request: 1
11:32:37.313  main             (1) subscriber.next: brown
11:32:37.313  main             subscription.request: 1
11:32:37.313  main             (1) subscriber.next: fox
11:32:37.313  main             subscription.request: 1
11:32:37.313  main             (1) subscriber.next: jumps
11:32:37.313  main             subscription.request: 1
11:32:37.313  main             (1) subscriber.next: over
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (1) subscriber.next: the
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (1) subscriber.next: lazy
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (1) subscriber.next: dog.
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             subscription.terminate
11:32:37.314  main             (1) subscriber.complete
11:32:37.314  main             (2) subscriber.subscribe: SyncIteratorPublisher$SubscriptionImpl@3dd4520b
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: The
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: quick
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: brown
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: fox
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: jumps
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: over
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: the
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: lazy
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             (2) subscriber.next: dog.
11:32:37.314  main             subscription.request: 1
11:32:37.314  main             subscription.terminate
11:32:37.314  main             (2) subscriber.complete
```



## Conclusion

Reactive streams take a proper place among other parallel and concurrent Java frameworks. Before they appeared in the JDK, there were slightly related CompletableFuture and Stream APIs. CompletableFuture uses the _push_ model but supports asynchronous computations of a single value. Stream supports sequential or parallel computations of multiple values but uses the _pull_ model. Reactive streams have taken a vacant place that supports synchronous or asynchronous computations of multiple values and can additionally dynamically switch between the _push_ and _pull_ models. Reactive streams are suitable for processing possible unbounded sequences of events with unpredictable rates, such as mouse and keyboard events, sensor events, latency-bound I/O events from a file or network, and so on.

Application developers should not implement the interfaces of the Reactive Streams specification themselves. The specification is complex enough, especially in concurrent publisher-subscriber contracts to be implemented correctly. Also, the specification does not contain APIs for intermediate stream operations. They should use stream components (producers, processors, consumers) from existing frameworks and use the Reactive Streams API only to connect them. Then application developers should use the much richer native frameworks APIs.

Complete code examples are available in the GitHub repository.
