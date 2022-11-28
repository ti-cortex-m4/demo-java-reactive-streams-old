## The Reactive Streams specification

[Reactive Streams](https://www.reactive-streams.org/) is a specification to provide a standard for asynchronous stream processing with non-blocking back pressure for various runtime environments (JVM, .NET and JavaScript) as well as network protocols. The Reactive Streams specification for the JVM (the latest version 1.0.4 was released at May 26th, 2022) consists of the following parts:



* textual [specification](https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.4/README.md#specification)
* the Java [API](https://www.reactive-streams.org/reactive-streams-1.0.4-javadoc) that contains four interfaces that should be implemented according to this specification
* the Technology Compatibility Kit ([TCK](https://www.reactive-streams.org/reactive-streams-tck-1.0.4-javadoc)), a standard test suite for conformance testing of implementations
* [implementation examples](https://www.reactive-streams.org/reactive-streams-examples-1.0.4-javadoc)

Reactive Streams is not a trivial specification. This happened because the specification was created after there were several mature implementations of reactive streams.

Firstly, there is not enough to implement interfaces to make a reactive stream. To work correctly in an asynchronous environment, components of reactive streams must obey its contract. These contracts are summarized as textual specifications and verified in the Technology Compatibility Kit ([TCK](https://www.reactive-streams.org/reactive-streams-tck-1.0.4-javadoc)).

Secondly, the specification doesn’t contain any implementations. It was created to provide a minimal standard that should provide interoperability across already existing implementations of reactive streams. An application developer should rarely implement this specification on their own, but instead use components from existing implementations:



* general-purpose frameworks (Netflix RxJava, Lightbend Akka Streams, Pivotal Reactor, Eclipse Vert.x)
* web frameworks (RatPack)
* different back-end applications (Reactive Mongo, Reactive Rabbit)

Thirdly, this specification is limited. It covers only mediating the stream of data between components (producers, consumers, processing stages). Other routine stream operations (filter, map, reducing, splitting, merging, etc.) are not covered by this specification. An application developer should use this specification to select components from the existing implementations and then use their native APIs.


## The Reactive Streams API

The Reactive Streams API consists of the following interfaces that contains in package _org.reactivestreams_:



* Publisher&lt;T>: A producer of data and control events received by Subscribers
* Subscriber&lt;T>: A consumer of events
* Subscription: A connection linking a Publisher and Subscriber
* Processor&lt;T,R>: A component that acts as both a Subscriber and Publisher

![Reactive Streams API](/images/Reactive_Streams_API.png)


#### Publisher

The [Publisher](https://github.com/reactive-streams/reactive-streams-jvm#1-publisher-code) interface represents a producer of a potentially unbounded number of data and control events. A Publisher sends items according to the demand received from one or many Subscribers. Subscribers can subscribe and unsubscribe dynamically at various points in time. Each active Subscriber receives the same events in the same order, unless drops or errors are encountered.


```
public interface Publisher<T> {
    public void subscribe(Subscriber<? super T> s);
}
```


This interface has the following method:



* The method _Publisher#subscribe(Subscriber)_ requests a Publisher to start sending data to a Subscriber

Publishers may vary about whether Subscribers receive items that were produced before they subscribed. Those producers who can be repeated and do not start until subscribed to, are _cold_ producers (examples: in-memory iterators, file readings, database queries). Those producers who cannot be repeated and start immediately regardless of whether it has subscribers, are _hot_ producers (examples: keyboard and mouse events, sensor events, network requests, time).


#### Subscriber

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
* The _onComplete()_ method is invoked at successful completion
* The _onError(Throwable)_ method is invoked at failed completion (upon an unrecoverable error encountered by a Publisher or Subscription)

After invocation of _Subscriber#onError(Throwable)_ or _Subscriber#onComplete()_ no other events will be sent to the Subscriber by the current Subscription.


#### Subscription

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

A canceled Subscription does not receive _Subscriber#onComplete() _or _Subscriber#onError(Throwable) _events.


#### Processor

The [Processor](https://github.com/reactive-streams/reactive-streams-jvm#4processor-code) interface represents a processing stage, which is both a Subscriber and a Publisher and obeys the contracts of both. A Processor works as a Consumer for a previous stage (a Publisher or a previous Processor) and as a Producer for a next stage (a next Processor or a Consumer).


```
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```


The Processor interface is intended to implement intermediate stream operations (filter, map, reducing, splitting, merging, etc.)


## Workflow

When a Subscriber wants to start receiving items from a Publisher, it calls the _Publisher#subscribe(Subscriber)_ method and passes itself as a parameter to be stored in a Subscription later. If the Publisher accepts the request from the Subscriber, then it creates a new Subscription object and invokes the _Subscriber#onSubscribe(Subscription)_ method. If the Publisher rejects the request of the Subscriber or otherwise fails (for example, already subscribed), it invokes _Subscriber#onError(Throwable)_ method.

After setting the relation between the Publisher and the Subscriber through the Subscription object, the Subscriber can request items and the Publisher can return them. When the Subscriber wants to receive items, it calls the _Subscription#request(long)_ method with a number of requested items. Typically, the first such call occurs in the _Subscriber#onSubscribe_ method. If the Subscriber wants to stop receiving items, it calls the _Subscription#cancel()_ method. After invoking this method the Subscriber may still receive items to meet previously requested demand.

The Producer sends each requested item by calling the _Subscriber#onNext(T)_ method only in response to the previous call the _Subscription#request(long)_ method but never by its own. A Publisher can send less than is requested if the stream ends but then must emit either _Subscriber#onError(Throwable)_ or _Subscriber#onComplete()_.

When there are no more items, the Publisher completes the Subscription _normally _by calling the _Subscriber#onCompleted()_ method. When in the Producer happens an unrecoverable exception, the Publisher completes the Subscription exceptionally by calling the _Subscriber#onError(Throwable)_ method.
