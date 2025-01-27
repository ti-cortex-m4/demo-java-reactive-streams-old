# Reactive Streams specification in Java


## Introduction

Reactive Streams is a cross-platform specification for processing a potentially infinite sequence of events across asynchronous boundaries (threads, processes, or network-connected computers) with non-blocking backpressure. A reactive stream contains a publisher, which sends forward _data_, _error_, _completion_ events, and subscribers, which send backward _request_ and _cancel_ backpressure events. There can also be intermediate processors between the publisher and the subscribers that filter or transform events.

<sub><em>Backpressure</em> is application-level flow control from the subscriber to the publisher to control the sending rate.</sub>

The Reactive Streams specification is designed to efficiently process (in terms of CPU and memory usage) time-ordered sequences of events. For efficient CPU usage, the specification describes the contracts for asynchronous and non-blocking events processing in different stages (producers, processors, consumers). For efficient memory usage, the specification describes the contracts for switching between _push_ and _pull_ communication models based on the events processing rate, which avoids using unbounded buffers.


## Problems and solutions

When designing systems to transfer items from a producer to a consumer, the goal is to send them with minimal latency and maximum throughput.

<sub><em>Latency</em> is the time between sending an item from the producer and its receiving by the consumer. <em>Throughput</em> is the number of items sent from producer to consumer per unit of time.</sub>

However, the producer and the consumer may have limitations that can prevent the system from achieving the best performance:



* The consumer can be slower than the producer.
* The consumer may not be able to skip items that it does not have time to process.
* The producer may not be able to slow or stop sending items that the consumer does not have time to process.
* The producer and consumer may have a limited number of CPU cores to process items asynchronously and memory to buffer items.
* The communication channel between the producer and the consumer may have limited bandwidth.

There are several patterns for sequential item processing, that solve some or most of the above limitations:



* Iterator
* Observer
* Reactive Extensions
* Reactive Streams

These patterns fall into two groups: synchronous _pull_ communication models (in which the consumer determines when to receive items from the producer) and asynchronous _push_ communication models (in which the producer determines when to send items to the consumer).


### Iterator

In the Iterator pattern, the consumer synchronously _pulls_ items from the producer one by one. The producer sends an item only when the consumer requests it. If the producer does not have an item at the time of the request, he sends an empty response.

![Iterator](/images/Iterator.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer cannot request the next item if he has not yet processed the previous one.
* The consumer can stop the exchange at any time.

Cons:



* The latency may not be optimal due to an incorrectly chosen pulling period (too long pulling period leads to high latency; too short pulling period wastes CPU and I/O resources).
* The throughput is not optimal because it takes one request-response to send each item.
* The consumer cannot determine if the producer has finished generating items.

When using the Iterator pattern, which transfers items one at a time, latency and throughput are often unsatisfactory. To improve these parameters with minimal changes, the same Iterator pattern can transfer items in batches rather than one at a time.

![Iterator with batching](/images/Iterator_with_batching.png)

Pros:



* Throughput increases as the number of requests/responses decreases from one for each item to one for all items in a batch.

Cons:



* The latency increases because the producer needs more time to send more items.
* If the batch size is too large, it may not fit in the memory of the producer or the consumer.
* If the consumer wants to stop processing, he can do so no sooner than he receives the entire batch.


### Observer

In the Observer pattern, one or many consumers subscribe to the producer's events. The producer asynchronously _pushes_ events to all subscribed consumers as soon as it generates them. The consumer can unsubscribe from the producer if it does not need further events.

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

In simplified terms, Reactive Extensions are a combination of the Observer and Iterator patterns and functional programming. From the Observer pattern, they took the consumer’s ability to subscribe to the producer’s events. From the Iterator pattern, they took the ability to handle event streams of three types (data, error, completion). From functional programming, they took the ability to handle event streams with chained methods (filter, transform, combine, etc.).

![Reactive Extensions](/images/Reactive_Extensions.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The consumer can determine when the producer has finished generating events.
* The latency is lower than in synchronous _pull_ communication models because the producer sends events to the consumer as soon as they become available.
* The consumer can uniformly handle event streams of three types (data, error, completion).
* Handling event streams with chained methods can be easier than with many nested event handlers.

Cons:



* A slower consumer may be overwhelmed by events from a faster producer.
* Implementing concurrent producers and consumers may be non-trivial.


### Reactive Streams

Reactive Streams are a further development of Reactive Extensions, which use backpressure to match producer and consumer performance. In simplified terms, Reactive Streams are a combination of Reactive Extensions and batching.

The main difference between them is who is the initiator of the exchange. In Reactive Extensions, a publisher sends events to a subscriber as soon as they become available and in any number. In Reactive Streams, a publisher must send events to a subscriber only after they have been requested and no more than the requested number.

![Reactive Streams](/images/Reactive_Streams.png)

Pros:



* The consumer can start the exchange at any time.
* The consumer can stop the exchange at any time.
* The consumer can determine when the producer has finished generating events.
* The latency is lower than in synchronous _pull_ communication models because the producer sends events to the consumer as soon as they become available.
* The consumer can uniformly handle event streams of three types (data, error, completion).
* Handling event streams with chained methods can be easier than with many nested event handlers.
* The consumer can request events from the producer depending on the need.

Cons:



* Implementing concurrent producers and consumers may be non-trivial.


## Backpressure

There are several solutions for the problem where a producer generates events faster than a consumer processes them. This does not happen in _pull_ communication models because the consumer initiates the exchange. In _push_ communication models, the producer cannot usually determine the sending rate, so the consumer may eventually receive more events than it can process. Backpressure is a solution to this problem by informing the producer about the processing rate of its consumers.

Without the use of backpressure, the consumer has a few solutions to deal with excessive events:



* buffer events
* drop events
* drop events and request the producer to resend them by their identifiers

<sub>Any solution that includes dropping events on the consumer may be inefficient because these events still require I/O operations to send them from the producer.</sub>

The backpressure in Reactive Streams is implemented as follows. To start receiving events from the producer, the consumer _pulls_ the number of items it wants to receive. Only then does the producer _push_ events to the consumer; the producer never sends them on its initiative. After the consumer has processed all the requested events, the whole cycle repeats. In a particular case, if the consumer is known to be faster than the producer, it can work in the _push_ communication model and request all items immediately after subscribing. Or vice versa, if the consumer is known to be slower than the producer, it can work in the _pull_ communication model and request the next items only after the previous ones have been processed. Thus, the model in which reactive streams operate can be described as a _dynamic push/pull_ communication model. It works effectively if the producer is faster or slower than the consumer or even when that ratio can change over time.

With the use of backpressure, the producer has much more solutions to deal with excessive events:



* buffer events
* drop events
* pause generation events
* block the producer
* cancel the event stream

Which solutions to use for a particular reactive stream depends on the nature of the events. But backpressure is not a _silver bullet_. It simply shifts the problem of performance mismatch to the producer's side, where it is supposed to be easier to solve. However, in some cases, there are better solutions than backpressure, such as simply dropping excessive events on the consumer's side.


## The Reactive Streams specification

Reactive Streams is a [specification](https://www.reactive-streams.org/) to provide a standard for asynchronous stream processing with non-blocking backpressure for various runtime environments (JVM, .NET, and JavaScript) and network protocols. The Reactive Streams specification was created by engineers from Kaazing, Lightbend, Netflix, Pivotal, Red Hat, Twitter, and others.

The specification describes the concept of _reactive streams_ that have the following features:



* reactive streams can be _unicast_ and _multicast_: a publisher can send events to one or many consumers.
* reactive streams are potentially _unbounded_: they can handle zero, one, many, or an infinite number of events.
* reactive streams are _sequential_: a consumer processes events in the same order in which a producer sends them.
* reactive streams can be _synchronous_ or _asynchronous_: they can use computing resources for parallel processing in separate stages.
* reactive streams are _non-blocking_: they do not waste computing resources if the performance of a producer and a consumer are different.
* reactive streams use _mandatory backpressure_: a consumer can request events from a producer according to their processing rate.
* reactive streams use _bounded buffers_: they can be implemented without unbounded buffers, avoiding memory out-of-memory errors.

The Reactive Streams [specification for the JVM](https://github.com/reactive-streams/reactive-streams-jvm) (the latest version 1.0.4 was released on May 26th, 2022) contains the textual specification and the Java API, which contains four interfaces that must be implemented according to this specification. It also includes the Technology Compatibility Kit (TCK), a standard test suite for conformance testing of implementations.

It is important to note that the Reactive Streams specification was created _after_ several mature but incompatible implementations of Reactive Streams already existed. Therefore, the specification is currently limited and contains only low-level APIs. Application developers should use this specification to provide interoperability between existing implementations. To have high-level functional APIs (filter, transform, combine, etc.), application developers should use implementations of this specification (Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava, etc.) through their native APIs.


## The Reactive Streams API

The Reactive Streams API consists of four interfaces, which are located in the _org.reactivestreams_ package:



* The Publisher&lt;T> interface represents a producer of data and control events.
* The Subscriber&lt;T> interface represents a consumer of events.
* The Subscription interface represents a connection between a Publisher and a Subscriber.
* The Processor&lt;T,R> interface represents a processor of events that acts as both a Subscriber and a Publisher.

![Reactive Streams API](/images/Reactive_Streams_API.png)


### Publisher

The Publisher interface represents a producer of potentially infinite sequenced data and control events. A Publisher produces events according to the demand received from one or many Subscribers.

<sub><em>Demand</em> is the aggregated number of items requested by a Subscriber that have not yet been delivered by the Publisher.</sub>

Publishers may vary about whether Subscribers receive events that were produced before they subscribed. _Cold_ publishers can be repeated and do not start until they are subscribed (in-memory iterators, file readings, database queries, etc.). _Hot_ publishers cannot be repeated and start immediately, regardless of the presence of subscribers (keyboard and mouse events, sensor events, network requests, etc.).


```java
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```


This interface has the following method:



* The _subscribe(Subscriber)_ method requests the Publisher to start sending events to a Subscriber.


### Subscriber

The Subscriber interface represents a consumer of events. Multiple Subscribers can subscribe to and unsubscribe from a Producer at different times.


```java
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


```java
public interface Subscription {
    public void request(long n);
    public void cancel();
}
```


This interface has the following methods:



* The _request(long)_ method adds the given number of items to the unfulfilled demand for this Subscription.
* The _cancel()_ method requests the Publisher to _eventually_ stop sending items.


### Processor

The Processor interface represents a processing stage that extends the Subscriber and Publisher interfaces and is subject to the contracts of both. It acts as a Subscriber for the previous stage of a reactive stream and as a publisher for the next one.


```java
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```



## The Reactive Streams workflow

The Reactive Streams workflow consists of three steps: establishing a connection, exchanging data and control events, and successfully or exceptionally terminating the connection.

![Reactive Streams workflow](/images/Reactive_Streams_workflow.png)

When a Subscriber wants to start receiving events from a Publisher, it calls the _Publisher.subscribe(Subscriber)_ method. If the Publisher accepts the request, it creates a new Subscription instance and invokes the _Subscriber.onSubscribe(Subscription)_ method. If the Publisher rejects the request or otherwise fails, it invokes the _Subscriber.onError(Throwable)_ method.

Once the Publisher and the Subscriber establish a connection with each other through the Subscription instance, the Subscriber can request events, and the Publisher can send them. When the Subscriber wants to receive events, it calls the _Subscription#request(long)_ method with the number of items requested. Typically, the first such call occurs in the _Subscriber.onSubscribe(Subscription)_ method. The Publisher sends each requested item by calling the _Subscriber.onNext(T)_ method only in response to a previous request. A Publisher can send fewer events than requested if the reactive stream ends, but then must call either the _Subscriber.onComplete()_ or _Subscriber.onError(Throwable)_ methods.

If the Subscriber wants to stop receiving events, it calls the _Subscription.cancel()_ method. After calling this method, the Subscriber can continue to receive events to meet the previously requested demand. A canceled Subscription does not receive _Subscriber.onComplete()_ or _Subscriber.onError(Throwable)_ events.

When there are no more events, the Publisher completes the Subscription successfully by calling the _Subscriber.onCompleted()_ method. When an unrecoverable exception occurs in the Publisher, it completes the Subscription exceptionally by calling the _Subscriber.onError(Throwable)_ method. After invocation of _Subscriber.onComplete()_ or _Subscriber.onError(Throwable)_ events, the current Subscription will not send any other events to the Subscriber.


## The JDK Flow API

The JDK has supported the Reactive Streams specification since version 9 in the form of the Flow API. The [Flow](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Flow.html) class contains nested static interfaces Publisher, Subscriber, Subscription, Processor, which are 100% semantically equivalent to their respective Reactive Streams counterparts. The Reactive Streams specification contains the [FlowAdapters](https://github.com/reactive-streams/reactive-streams-jvm/blob/master/api/src/main/java9/org/reactivestreams/FlowAdapters.java) class, which is a bridge between the Reactive Streams API (the _org.reactivestreams_ package) and the JDK Flow API (the _java.util.concurrent.Flow_ class). The only implementation of the Reactive Streams specification that JDK provides so far is the [SubmissionPublisher](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/SubmissionPublisher.html) class that implements the Publisher interface.


## Code examples


### Cold synchronous reactive stream

The following class demonstrates a synchronous Publisher that sends a finite sequence of events from an Iterator. The _synchronous_ Publisher processes its _subscribe_ event, and the Subscription’s events _request_ and _cancel_ in the caller thread. This _multicast_ Publisher can send events to multiple Subscribers, storing information about each connection in a nested private implementation of the Subscription interface. The Subscription implementation contains the current Iterator instance, the demand, and the cancellation flag. To make a _cold_ Publisher that sends the same sequence of events for each Subscriber, the Publisher stores a Supplier that must return a new Iterator instance for each invocation. The Publisher uses different types of error handling (throwing an exception or calling the Subscriber _onError_ method) according to the Reactive Streams specification.


```java
public class SyncIteratorPublisher<T> implements Flow.Publisher<T> {

   private final Supplier<Iterator<? extends T>> iteratorSupplier;

   public SyncIteratorPublisher(Supplier<Iterator<? extends T>> iteratorSupplier) {
       this.iteratorSupplier = Objects.requireNonNull(iteratorSupplier);
   }

   @Override
   public void subscribe(Flow.Subscriber<? super T> subscriber) {
       // By rule 1.11, a Publisher may support multiple Subscribers and decide
       // whether each Subscription is unicast or multicast.
       new SubscriptionImpl(subscriber);
   }

   private class SubscriptionImpl implements Flow.Subscription {

       private final Flow.Subscriber<? super T> subscriber;
       private final Iterator<? extends T> iterator;
       private final AtomicLong demand = new AtomicLong(0);
       private final AtomicBoolean cancelled = new AtomicBoolean(false);

       SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
           // By rule 1.9, calling Publisher.subscribe(Subscriber)
           // must throw a NullPointerException when the given parameter is null.
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

           // By rule 3.9, while the Subscription is not cancelled, Subscription.request(long)
           // must signal onError with a IllegalArgumentException if the argument is <= 0.
           if ((n <= 0) && !cancelled.get()) {
               doCancel();
               subscriber.onError(new IllegalArgumentException("non-positive subscription request"));
               return;
           }

           for (;;) {
               long oldDemand = demand.get();
               if (oldDemand == Long.MAX_VALUE) {
                   // By rule 3.17, a demand equal or greater than Long.MAX_VALUE
                   // may be considered by the Publisher as "effectively unbounded".
                   return;
               }

               // By rule 3.8, while the Subscription is not cancelled, Subscription.request(long)
               // must register the given number of additional elements to be produced to the respective Subscriber.
               long newDemand = oldDemand + n;
               if (newDemand < 0) {
                   // By rule 3.17, a Subscription must support a demand up to Long.MAX_VALUE.
                   newDemand = Long.MAX_VALUE;
               }

               // By rule 3.3, Subscription.request must place an upper bound on possible synchronous recursion
               // between Publisher and Subscriber.
               if (demand.compareAndSet(oldDemand, newDemand)) {
                   if (oldDemand > 0) {
                       return;
                   }
                   break;
               }
           }

           // By rule 1.2, a Publisher may signal fewer onNext than requested
           // and terminate the Subscription by calling onError.
           for (; demand.get() > 0 && iterator.hasNext() && !cancelled.get(); demand.decrementAndGet()) {
               try {
                   subscriber.onNext(iterator.next());
               } catch (Throwable t) {
                   if (!cancelled.get()) {
                       // By rule 1.6, if a Publisher signals onError on a Subscriber,
                       // that Subscriber's Subscription must be considered cancelled.
                       doCancel();
                       // By rule 1.4, if a Publisher fails it must signal an onError.
                       subscriber.onError(t);
                   }
               }
           }

           // By rule 1.2, a Publisher may signal fewer onNext than requested
           // and terminate the Subscription by calling onComplete.
           if (!iterator.hasNext() && !cancelled.get()) {
               // By rule 1.6, if a Publisher signals onComplete on a Subscriber,
               // that Subscriber's Subscription must be considered cancelled.
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
           cancelled.set(true);
       }

       private void doError(Throwable t) {
           // By rule 1.6, if a Publisher signals onError on a Subscriber,
           // that Subscriber's Subscription must be considered cancelled.
           cancelled.set(true);
           subscriber.onError(t);
       }
   }
}
```


The following class demonstrates a synchronous Subscriber that _pulls_ items one by one. The _synchronous_ Subscriber processes its _onSubscribe_, _onNext_, _onError_, _onComplete_ events in the Publisher thread. Like the Publisher, the Subscriber also stores its Subscription (to perform backpressure) and its cancellation flag. The Subscriber uses different types of error handling (throwing an exception or unsubscribing) according to the Reactive Streams specification.


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
           // By rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription
           // after an onSubscribe signal if it already has an active Subscription.
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

       // By rule 2.8, a Subscriber must be prepared to receive one or more onNext signals
       // after having called Subscription.cancel()
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

       // By rule 2.4, Subscriber.onError(Throwable) must consider the Subscription cancelled
       // after having received the signal.
       cancelled.set(true);
       whenError(t);
   }

   @Override
   public void onComplete() {
       logger.info("({}) subscriber.complete", id);

       // By rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled
       // after having received the signal.
       cancelled.set(true);
       whenComplete();
   }

   public void awaitCompletion() throws InterruptedException {
       completed.await();
   }

   // This method is invoked when OnNext signals arrive and returns whether more elements are desired.
   protected boolean whenNext(T item) {
       return true;
   }

   // This method is invoked when an OnError signal arrives.
   protected void whenError(Throwable t) {
   }

   // This method is invoked when an OnComplete signal arrives.
   protected void whenComplete() {
       completed.countDown();
   }

   private void doCancel() {
       cancelled.set(true);
       subscription.get().cancel();
   }
}
```


<sub>The GitHub repository has <a href="https://github.com/aliakh/demo-java-reactive-streams/tree/main/src/test/java/demo/reactivestreams/part1">unit tests</a> to verify that the Publisher and Subscriber comply with all the Reactive Streams specification contracts that its TCK checks.</sub>

The following code fragment demonstrates that this synchronous Publisher transfers the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to these two synchronous Subscribers.


```java
List<String> words = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
Supplier<Iterator<? extends String>> iteratorSupplier = () -> List.copyOf(words).iterator();
SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(iteratorSupplier);

SyncSubscriber<String> subscriber1 = new SyncSubscriber<>(1);
publisher.subscribe(subscriber1);

SyncSubscriber<String> subscriber2 = new SyncSubscriber<>(2);
publisher.subscribe(subscriber2);

subscriber1.awaitCompletion();
subscriber2.awaitCompletion();
```


The invocation log of this code fragment shows that the synchronous Publisher sends the sequence of events in one thread, and the synchronous Subscribers receive these events in the same thread _one at a time_.


```
12:00:00.034  main                              (1) subscriber.subscribe: demo.reactivestreams.part1.SyncIteratorPublisher$SubscriptionImpl@7791a895
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: The
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: quick
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: brown
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: fox
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: jumps
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: over
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: the
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: lazy
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.next: dog.
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (1) subscriber.complete
12:00:00.037  main                              (2) subscriber.subscribe: demo.reactivestreams.part1.SyncIteratorPublisher$SubscriptionImpl@610694f1
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: The
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: quick
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: brown
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: fox
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: jumps
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: over
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: the
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: lazy
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.next: dog.
12:00:00.037  main                              subscription.request: 1
12:00:00.037  main                              (2) subscriber.complete
```



### Cold asynchronous reactive stream

Examples of an asynchronous Producer and an asynchronous Consumer are provided in a separate [document](https://github.com/aliakh/demo-java-reactive-streams/tree/main/src/main/java/demo/reactivestreams/part2).


### Hot asynchronous reactive stream

The following class demonstrates an asynchronous Publisher that sends an infinite sequence of events from a [WatchService](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/WatchService.html) implementation for a file system. The _asynchronous_ Publisher is inherited from the [SubmissionPublisher](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/SubmissionPublisher.html) class and reuses its Executor for asynchronous invocations. This Publisher is _hot_ and sends events about file changes in the given folder.


```java
public class WatchServiceSubmissionPublisher extends SubmissionPublisher<WatchEvent<Path>> {

   private final Future<?> task;

   WatchServiceSubmissionPublisher(String folderName) {
       ExecutorService executorService = (ExecutorService) getExecutor();

       task = executorService.submit(() -> {
           try {
               WatchService watchService = FileSystems.getDefault().newWatchService();

               Path folder = Paths.get(folderName);
               folder.register(watchService,
                   StandardWatchEventKinds.ENTRY_CREATE,
                   StandardWatchEventKinds.ENTRY_MODIFY,
                   StandardWatchEventKinds.ENTRY_DELETE
               );

               WatchKey key;
               while ((key = watchService.take()) != null) {
                   for (WatchEvent<?> event : key.pollEvents()) {
                       WatchEvent.Kind<?> kind = event.kind();
                       if (kind == StandardWatchEventKinds.OVERFLOW) {
                           continue;
                       }

                       WatchEvent<Path> watchEvent = (WatchEvent<Path>) event;

                       logger.info("publisher.submit: path {}, action {}", watchEvent.context(), watchEvent.kind());
                       submit(watchEvent);
                   }

                   boolean valid = key.reset();
                   if (!valid) {
                       break;
                   }
               }

               watchService.close();
           } catch (IOException | InterruptedException e) {
               throw new RuntimeException(e);
           }
       });
   }

   @Override
   public void close() {
       logger.info("publisher.close");
       task.cancel(false);
       super.close();
   }
}
```


The following class demonstrates an asynchronous Processor that filters events (by the given file extension) and transforms them (from WatchEvent to String). As a Subscriber, this Processor implements the Subscriber methods _onSubscribe_, _onNext_, _onError_, _onComplete_ to connect to an upstream Producer and _pull_ items one by one. As a Publisher, this Processor uses the SubmissionPublisher _submit_ method to send items to a downstream Subscriber.


```java
public class WatchEventSubmissionProcessor extends SubmissionPublisher<String>
   implements Flow.Processor<WatchEvent<Path>, String> {

   private final String fileExtension;

   private Flow.Subscription subscription;

   public WatchEventSubmissionProcessor(String fileExtension) {
       this.fileExtension = fileExtension;
   }

   @Override
   public void onSubscribe(Flow.Subscription subscription) {
       logger.info("processor.subscribe: {}", subscription);
       this.subscription = subscription;
       this.subscription.request(1);
   }

   @Override
   public void onNext(WatchEvent<Path> watchEvent) {
       logger.info("processor.next: path {}, action {}", watchEvent.context(), watchEvent.kind());
       if (watchEvent.context().toString().endsWith(fileExtension)) {
           submit(String.format("file %s is %s", watchEvent.context(), decode(watchEvent.kind())));
       }
       subscription.request(1);
   }

   @Override
   public void onError(Throwable t) {
       logger.error("processor.error", t);
       closeExceptionally(t);
   }

   @Override
   public void onComplete() {
       logger.info("processor.completed");
       close();
   }
}
```


The following code fragment demonstrates that this Publisher generates events about changes in the current user's home directory, this Processor filters the events related to text files and transforms them to Strings, and the synchronous Subscriber mentioned earlier logs them.


```java
String folderName = System.getProperty("user.home");
String fileExtension = ".txt";

try (SubmissionPublisher<WatchEvent<Path>> publisher = new WatchServiceSubmissionPublisher(folderName);
    WatchEventSubmissionProcessor processor = new WatchEventSubmissionProcessor(fileExtension)) {

   SyncSubscriber<String> subscriber = new SyncSubscriber<>();
   processor.subscribe(subscriber);
   publisher.subscribe(processor);

   TimeUnit.SECONDS.sleep(60);

   publisher.close();

   subscriber.awaitCompletion();
}
```


The invocation log of this code fragment shows that the Publisher and the Processor use the internal implementation of the Subscription interface, the nested package-private SubmissionPublisher.BufferedSubscription class. These classes process events asynchronously by default in the worker threads of the common Fork/Join thread pool.


```
12:00:00.052  ForkJoinPool.commonPool-worker-3  processor.subscribe: java.util.concurrent.SubmissionPublisher$BufferedSubscription@6f753c95
12:00:00.052  ForkJoinPool.commonPool-worker-2  subscriber.subscribe: java.util.concurrent.SubmissionPublisher$BufferedSubscription@1668d43d
12:00:10.058  ForkJoinPool.commonPool-worker-1  publisher.submit: path example.txt, action ENTRY_CREATE
12:00:10.058  ForkJoinPool.commonPool-worker-4  processor.next: path example.txt, action ENTRY_CREATE
12:00:10.064  ForkJoinPool.commonPool-worker-5  subscriber.next: file example.txt is created
12:00:20.072  ForkJoinPool.commonPool-worker-1  publisher.submit: path example.txt, action ENTRY_MODIFY
12:00:20.073  ForkJoinPool.commonPool-worker-5  processor.next: path example.txt, action ENTRY_MODIFY
12:00:20.073  ForkJoinPool.commonPool-worker-1  publisher.submit: path example.txt, action ENTRY_MODIFY
12:00:20.073  ForkJoinPool.commonPool-worker-4  subscriber.next: file example.txt is modified
12:00:20.073  ForkJoinPool.commonPool-worker-2  processor.next: path example.txt, action ENTRY_MODIFY
12:00:20.073  ForkJoinPool.commonPool-worker-6  subscriber.next: file example.txt is modified
12:00:30.085  ForkJoinPool.commonPool-worker-1  publisher.submit: path example.txt, action ENTRY_DELETE
12:00:30.086  ForkJoinPool.commonPool-worker-6  processor.next: path example.txt, action ENTRY_DELETE
12:00:30.086  ForkJoinPool.commonPool-worker-6  subscriber.next: file example.txt is deleted
12:01:00.057  main                              publisher.close
12:01:00.057  ForkJoinPool.commonPool-worker-7  processor.completed
12:01:00.058  ForkJoinPool.commonPool-worker-7  subscriber.complete
12:01:00.058  main                              publisher.close
```



## Conclusion

Before Reactive Streams appeared in the JDK, there were related CompletableFuture and Stream APIs. The CompletableFuture API uses the _push_ communication model but supports asynchronous computations of a single value. The Stream API supports synchronous or asynchronous computations of multiple values but uses the _pull_ communication model. Reactive Streams have taken a vacant place and support synchronous or asynchronous computations of multiple values and can also dynamically switch between the _push_ and _pull_ computations models. Therefore, Reactive Streams are suitable for processing sequences of events with unpredictable rates, such as mouse and keyboard events, sensor events, and latency-bound I/O events from a file or network.

Crucially, application developers should not implement the interfaces of the Reactive Streams specification themselves. First, the specification is complex enough, especially in asynchronous contracts, and cannot be easily implemented correctly. Second, the specification does not contain APIs for intermediate stream operations. Instead, application developers should implement the reactive stream stages (producers, processors, consumers) using existing frameworks (Lightbend Akka Streams, Pivotal Project Reactor, Netflix RxJava) with their much richer native APIs. They should use the Reactive Streams API only to combine heterogeneous stages into a single reactive stream.

Complete code examples are available in the [GitHub repository](https://github.com/aliakh/demo-java-reactive-streams).
