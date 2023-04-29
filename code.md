<!-- Output copied to clipboard! -->

<!-----

Yay, no errors, warnings, or alerts!

Conversion time: 0.511 seconds.


Using this Markdown file:

1. Paste this output into your source file.
2. See the notes and action items below regarding this conversion run.
3. Check the rendered output (headings, lists, code blocks, tables) for proper
   formatting and use a linkchecker before you publish this page.

Conversion notes:

* Docs to Markdown version 1.0β34
* Sat Apr 29 2023 01:39:05 GMT-0700 (PDT)
* Source doc: #3
* This is a partial selection. Check to make sure intra-doc links work.
----->



## Code examples


### Synchronous publisher and subscriber

The following code example demonstrates a synchronous Producer that sends a finite sequence of items from a given Iterator.

The following code sample demonstrates a synchronous Consumer that _pulls_ items one by one and logs received events.

The following code example demonstrates that the multicast Producer sends the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to multiple Subscribers.


```
List<String> list = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
SyncIteratorPublisher<String> publisher = new SyncIteratorPublisher<>(() -> List.copyOf(list).iterator());

SyncSubscriber<String> subscriber1 = new SyncSubscriber<>(1);
publisher.subscribe(subscriber1);

SyncSubscriber<String> subscriber2 = new SyncSubscriber<>(2);
publisher.subscribe(subscriber2);

subscriber1.awaitCompletion();
subscriber2.awaitCompletion();
```


The following log demonstrates that the synchronous Producer sends a sequence of events in the caller's thread, and the synchronous Consumers receive the sequence of events in the Producer's thread _in sequence_.


```
11:32:37.310  main             (1) subscriber.subscribe: demo.reactivestreams.demo.demo1.SyncIteratorPublisher$SubscriptionImpl@1f28c152
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
11:32:37.314  main             (2) subscriber.subscribe: demo.reactivestreams.demo.demo1.SyncIteratorPublisher$SubscriptionImpl@3dd4520b
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



### Asynchronous publisher and subscriber

The following code example demonstrates an asynchronous Producer that sends a finite sequence of items from a given Iterator.

The following code sample demonstrates an asynchronous Consumer that _pulls_ items one by one and logs received events.

The following code example demonstrates that the multicast Producer sends the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to multiple Subscribers.


```
ExecutorService executorService = Executors.newFixedThreadPool(3);

List<String> list = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
AsyncIteratorPublisher<String> publisher = new AsyncIteratorPublisher<>(() -> List.copyOf(list).iterator(), 1024, executorService);

AsyncSubscriber<String> subscriber1 = new AsyncSubscriber<>(1, executorService);
publisher.subscribe(subscriber1);

AsyncSubscriber<String> subscriber2 = new AsyncSubscriber<>(2, executorService);
publisher.subscribe(subscriber2);

subscriber1.awaitCompletion();
subscriber2.awaitCompletion();

executorService.shutdown();
executorService.awaitTermination(60, TimeUnit.SECONDS);
```


The following log shows that an asynchronous Producer sends a sequence of events in a separate thread, and that asynchronous Consumers receive a sequence of events in a separate thread _in parallel_.


```
11:33:22.089  pool-1-thread-2  (2) subscriber.subscribe: AsyncIteratorPublisher$SubscriptionImpl@1627882a
11:33:22.089  pool-1-thread-1  (1) subscriber.subscribe: AsyncIteratorPublisher$SubscriptionImpl@5eb92a60
11:33:22.092  pool-1-thread-1  subscription.request: 1
11:33:22.092  pool-1-thread-3  subscription.request: 1
11:33:22.093  pool-1-thread-3  (2) subscriber.next: The
11:33:22.093  pool-1-thread-1  (1) subscriber.next: The
11:33:22.093  pool-1-thread-3  subscription.request: 1
11:33:22.093  pool-1-thread-1  subscription.request: 1
11:33:22.093  pool-1-thread-3  (1) subscriber.next: quick
11:33:22.093  pool-1-thread-2  (2) subscriber.next: quick
11:33:22.093  pool-1-thread-3  subscription.request: 1
11:33:22.093  pool-1-thread-2  subscription.request: 1
11:33:22.093  pool-1-thread-3  (1) subscriber.next: brown
11:33:22.093  pool-1-thread-2  (2) subscriber.next: brown
11:33:22.093  pool-1-thread-3  subscription.request: 1
11:33:22.093  pool-1-thread-2  subscription.request: 1
11:33:22.093  pool-1-thread-3  (1) subscriber.next: fox
11:33:22.093  pool-1-thread-2  (2) subscriber.next: fox
11:33:22.093  pool-1-thread-3  subscription.request: 1
11:33:22.093  pool-1-thread-2  subscription.request: 1
11:33:22.094  pool-1-thread-3  (1) subscriber.next: jumps
11:33:22.094  pool-1-thread-2  (2) subscriber.next: jumps
11:33:22.094  pool-1-thread-3  subscription.request: 1
11:33:22.094  pool-1-thread-2  subscription.request: 1
11:33:22.094  pool-1-thread-3  (1) subscriber.next: over
11:33:22.094  pool-1-thread-2  subscription.request: 1
11:33:22.094  pool-1-thread-1  (2) subscriber.next: over
11:33:22.094  pool-1-thread-2  (1) subscriber.next: the
11:33:22.094  pool-1-thread-3  subscription.request: 1
11:33:22.094  pool-1-thread-2  subscription.request: 1
11:33:22.094  pool-1-thread-3  (2) subscriber.next: the
11:33:22.094  pool-1-thread-2  subscription.request: 1
11:33:22.094  pool-1-thread-3  (1) subscriber.next: lazy
11:33:22.094  pool-1-thread-2  (2) subscriber.next: lazy
11:33:22.094  pool-1-thread-3  subscription.request: 1
11:33:22.094  pool-1-thread-2  subscription.request: 1
11:33:22.094  pool-1-thread-3  (1) subscriber.next: dog.
11:33:22.094  pool-1-thread-2  (2) subscriber.next: dog.
11:33:22.094  pool-1-thread-2  (2) subscriber.complete
11:33:22.094  pool-1-thread-1  subscription.request: 1
11:33:22.094  pool-1-thread-1  subscription.request: 1
11:33:22.094  pool-1-thread-3  (1) subscriber.complete
```


The GitHub repository also has examples of how a synchronous Producer sends events to asynchronous Consumers and how an asynchronous Producer sends events to synchronous Consumers.
