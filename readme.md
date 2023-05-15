# Reactive Streams specification in Java


## Introduction

Reactive Streams is a cross-platform specification for processing a possibly unlimited sequence of events across asynchronous boundaries (threads, processes, or network-connected computers) with non-blocking backpressure. A reactive stream contains a publisher, which sends forward _data_, _error_, _completion_ events, and subscribers, which send backward _request_ and _cancel_ backpressure events. There can also be intermediate processors between the publisher and the subscribers that filter or modify events.

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
