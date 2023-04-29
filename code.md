## Code examples


### Synchronous publisher and subscriber

The following code example demonstrates a synchronous _cold_ Producer that sends a finite sequence of items from a given Iterator.

The following code sample demonstrates a synchronous Consumer that _pulls_ items one by one and logs received events. The comments show which code fragments are responsible for implementing which rules of the Reactive Streams specification. The GitHub repository also has blackbox and whitebox unit tests to verify that this consumer meets the specification using its TCK.


```
public class SyncSubscriber<T> implements Flow.Subscriber<T> {

   private final int id;
   private final CountDownLatch completed = new CountDownLatch(1);

   private Flow.Subscription subscription;
   private boolean cancelled = false;

   public SyncSubscriber(int id) {
       this.id = id;
   }

   @Override
   public void onSubscribe(Flow.Subscription subscription) {
       logger.info("({}) subscriber.subscribe: {}", id, subscription);
       // by rule 2.13, calling onSubscribe must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(subscription);

       if (this.subscription != null) {
           // by_rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription after an onSubscribe signal if it already has an active Subscription.
           subscription.cancel();
       } else {
           this.subscription = subscription;
           // by_rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
           this.subscription.request(1);
       }
   }

   @Override
   public void onNext(T item) {
       logger.info("({}) subscriber.next: {}", id, item);
       // by rule 2.13, calling onNext must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(item);

       // by_rule 2.8, a Subscriber must be prepared to receive one or more onNext signals after having called Subscription.cancel()
       if (!cancelled) {
           if (whenNext(item)) {
               // by_rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
               subscription.request(1);
           } else {
               // by_rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
               doCancel();
           }
       }
   }

   @Override
   public void onError(Throwable throwable) {
       logger.error("({}) subscriber.error", id, throwable);
       // by rule 2.13, calling onError must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(throwable);

       // by_rule 2.4, Subscriber.onError(Throwable t) must consider the Subscription cancelled after having received the signal.
       cancelled = true;
       whenError(throwable);
   }

   @Override
   public void onComplete() {
       logger.info("({}) subscriber.complete", id);

       // by_rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
       cancelled = true;
       whenComplete();
   }

   public void awaitCompletion() throws InterruptedException {
       completed.await();
   }

   // this method is invoked when OnNext signals arrive and returns whether more elements are desired or not, intended to be overridden.
   protected boolean whenNext(T item) {
       return true;
   }

   // this method is invoked when an OnError signal arrives, intended to be overridden.
   protected void whenError(Throwable throwable) {
   }

   // this method is invoked when an OnComplete signal arrives, intended to be overridden.
   protected void whenComplete() {
       completed.countDown();
   }

   private void doCancel() {
       cancelled = true;
       subscription.cancel();
   }
}
```


The following code example demonstrates that the _multicast_ Producer sends the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to multiple Subscribers.


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



### Asynchronous publisher and subscriber

The following code example demonstrates an asynchronous _cold_ Producer that sends a finite sequence of items from a given Iterator.

The following code sample demonstrates an asynchronous Consumer that _pulls_ items one by one and logs received events. The comments show which code fragments are responsible for implementing which rules of the Reactive Streams specification. The GitHub repository also has blackbox and whitebox unit tests to verify that this consumer meets the specification using its TCK.

The following code example demonstrates that the _multicast_ Producer sends the same sequence of events (the same pangram) to multiple Subscribers.


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
