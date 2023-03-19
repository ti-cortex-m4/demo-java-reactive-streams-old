## Architecture

### Iterator

In the Iterator pattern, the consumer synchronously _pulls_ data events from the producer one-by-one. The consumer determines when to start an exchange, when to request the next data event, and when to stop the exchange. The producer sends events only when the consumer requests it. If the producer has no event when it is requested, it sends an empty response.

Pros



* The consumer can start exchange at any time
* The consumer can request the next data event when it processed the previous one
* The consumer can stop exchange at any time

Cons



* The latency may be non-optimal because of an incorrectly chosen pulling period (too long pulling period results in high latency, too short pulling period results in wasted CPU and I/O resources)
* The throughput is non-optimal because it takes one request-response to send each data event
* A consumer can not determine if the producer has finished the evens generation

Iterator



* hasNext
* next


### Iterator with batching

In the Iterator with batching pattern, the consumer synchronously _pulls_ data events from the producer not one-by-one but in batches of fixed or variable size.

Pros:



* The consumer can start exchange at any time
* The consumer can request the next data events when it processed the previous ones
* The consumer can stop exchange at any time
* The throughput is increased because number of request-responses is reduced from _one_ per data event to one for _all_ data events in a batch

Cons



* The latency is increased, because the producer takes more time to generate more events
* If the batch size is too large, events may not fit in the memory of the producer or consumer
* If the consumer wants to stop processing, it can do this no sooner than it has received the entire events batch


### Observer

In the Observer pattern, one or many consumers subscribe to a producer's data events. The producer asynchronously _pushes_ data events to all subscribed consumers as soon as they become available. A consumer can unsubscribe from the producer at any time if it does not need further events.

Pros



* The consumer can start exchange at any time
* The consumer can stop exchange at any time
* The latency is low because producer sends events to the consumers as soon thew become available

Const



* A slow consumer (that did not process the previous events) than can overwhelmed with next data events from the faster producer
* A consumer can not determine if the producer has finished the data evens generation

Observable



* addObserver
* deleteObserver
* notifyObservers

Observer



* update


### Reactive Extensions

Reactive Extensions (ReactiveX) is a family of multi-platform frameworks to process synchronous or asynchronous streams, originally created by Microsoft. Implementation of Reactive Extensions for Java is the Netflix RxJava framework.

In a simplistic way, Reactive Extensions can be thought of as a combination of the Observer and Iterator patterns and functional programming. From the Observer pattern they took the ability of a consumer to subscribe to events of a producer. From the Iterator pattern they took the ability to process streams of events of three types (data, error, completion). From the functional programming they took the ability to process streams of events with chained methods in imperative style (filter, map, reduce, split, merge, etc.).

As the Iterator pattern has synchronous pull methods to handle data, error, and completion. Reactive Extensions has methods to do similar asynchronous push operations.


<table>
  <tr>
   <td>
   </td>
   <td>Iterator (pull)
   </td>
   <td>Observable (push)
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


Pros



* The consumer can start exchange at any time
* The consumer can stop exchange at any time
* A consumer can determine when the producer has finished the evens generation
* The latency is low because producer sends events to the consumers as soon thew become available
* A consumer can process of events of three types (data, error, completion) uniformly
* Processing streams of events with chained methods in imperative style can be simpler than processing nested event handlers

Const



* A slow consumer (that did not process the previous events) than can overwhelmed with next events from the faster producer

Observable



* subscribe

Observer



* onNext
* onError
* onComplete

Subscription



* unsubscribe


### Reactive Streams

Reactive Streams is an initiative to provide a standard for asynchronous stream processing with non-blocking backpressure.

Reactive Streams is the further development of Reactive Extensions that was designed to solve among others the problem of a slower consumer being overflown by events from a faster producer. In a simplistic way, Reactive Streams can be thought of as a combination of the Reactive Extensions and batching. Producer additionally got a method to set the number of events it wants to receive from the producer. This algorithm allows us to build systems that work equally efficiently regardless of whether the producer is faster or slower than the consumer, or even when the performance of the producer or consumer changes during the time.

Pros



* The consumer can start exchange at any time
* The consumer can stop exchange at any time
* A consumer can determine when the producer has finished the evens generation
* The latency is low because producer sends events to the consumers as soon thew become available
* A consumer can process of events of three types (data, error, completion) uniformly
* Processing streams of events with chained methods in imperative style can be simpler than processing nested event handlers
* The consumer can request events from the producer depending on its demand

Cons



* Implementation of concurrent producer can be not trivial

Publisher



* subscribe

Subscriber



* onSubscribe
* onNext
* onError
* onComplete

Subscription



* cancel
* request

## The Reactive Streams specification

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



* The _onSubscribe(Subscription)_ method is invoked when the Producer accepted the new Subscription after calling the _Publisher#subscribe(Subscriber)_ method
* The _onNext(T)_ method is invoked on each received item previously requested via the _Subscription#request(long)_ method
* The _onError(Throwable)_ method is invoked at failed completion (upon an unrecoverable error encountered by a Publisher or a Subscription)
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

When a Subscriber wants to start receiving items from a Publisher, it calls the _Publisher#subscribe(Subscriber)_ method and passes itself as a parameter to be stored in a Subscription later. If the Publisher accepts the request from the Subscriber, then it creates a new Subscription object and invokes the _Subscriber#onSubscribe(Subscription)_ method. If the Publisher rejects the request of the Subscriber or otherwise fails (for example, already subscribed), it invokes _Subscriber#onError(Throwable)_ method.

After setting the relation between the Publisher and the Subscriber through the Subscription object, the Subscriber can request items and the Publisher can produce them. When the Subscriber wants to receive items, it calls the _Subscription#request(long)_ method with a number of requested items. Typically, the first such call occurs in the _Subscriber#onSubscribe_ method. If the Subscriber wants to stop receiving items, it calls the _Subscription#cancel()_ method. After invoking this method the Subscriber may still receive items to meet previously requested demand.

<sub>A canceled Subscription does not receive <em>Subscriber#onComplete() </em>or <em>Subscriber#onError(Throwable) </em>events.</sub>

The Producer sends each requested item by calling the _Subscriber#onNext(T)_ method only in response to the previous call the _Subscription#request(long)_ method but never by its own. A Publisher can send less than is requested if the stream ends but then must emit either _Subscriber#onError(Throwable)_ or _Subscriber#onComplete()_.

<sub>After invocation of <em>Subscriber#onError(Throwable)</em> or <em>Subscriber#onComplete()</em> no other events will be sent to the Subscriber by the current Subscription.</sub>

When there are no more items, the Publisher completes the Subscription normally by calling the _Subscriber#onCompleted()_ method. When in the Producer happens an unrecoverable exception, the Publisher completes the Subscription exceptionally by calling the _Subscriber#onError(Throwable)_ method.

>To make a reactive stream _push_-based, a consumer can once invoke the _Subscription#request(long)_ method with parameter _Long.MAX_VALUE_. To make a reactive stream _pull_-based, a consumer can invoke the _Subscription#request(long)_ method with parameter _1_ each time it is ready.


## The JDK Flow API

Starting from Java 9, Reactive Streams have become a part of the JDK. In Java 9 was added the class [java.util.concurrent.Flow](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Flow.html) that contains nested interfaces _Publisher_, _Subscriber_, _Subscription_, _Processor _that are 100% semantically equivalent to their respective Reactive Streams counterparts. The only implementation of the Reactive Streams specification that JDK provides so far is the _java.util.concurrent.SubmissionPublisher_ class that implements the _Publisher_ interface.

<sub>Reactive Streams contains the class <em>org.reactivestreams.FlowAdapters</em> is a bridge between Reactive Streams API in the <em>org.reactivestreams</em> package and the Java 9 <em>java.util.concurrent.Flow</em> API.</sub>
