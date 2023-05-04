# Reactive Streams specification in Java


## Code examples


### Synchronous Publisher

The following code example demonstrates a synchronous Publisher that sends a finite sequence of events from Iterator. The _synchronous_ Publisher processes its _subscribe_ method and the Subscriptions’ _request_ and _cancel_ methods in the caller’s thread. This Publisher is _multicast_ and can send items to multiple Subscribers, storing information about each connection in a private implementation of the Subscription interface. This includes the current Iterator instance, the demand (the aggregated number of items requested by a Subscriber which is yet to be fulfilled by the Publisher) and the connection cancellation flag. To make a _cold_ Publisher that sends the same sequence of events for each Subscriber, the Publisher stores a Supplier that must return a new Iterator instance for each new Subscription. The Publisher uses different types of error handling (throwing an exception or calling the onError handler) according to the Reactive Streams specification.

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
       private final AtomicLong demand = new AtomicLong(0);
       private final AtomicBoolean cancelled = new AtomicBoolean(false);

       private Iterator<? extends T> iterator;

       SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
           // By rule 1.9, calling Publisher.subscribe must throw a NullPointerException when the given parameter is null.
           this.subscriber = Objects.requireNonNull(subscriber);

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
   private Flow.Subscription subscription;
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

       if (this.subscription != null) {
           // By rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription after an onSubscribe signal if it already has an active Subscription.
           subscription.cancel();
       } else {
           this.subscription = subscription;
           // By rule 2.1, a Subscriber must signal demand via Subscription.request(long) to receive onNext signals.
           this.subscription.request(1);
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
               subscription.request(1);
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
       subscription.cancel();
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



### Asynchronous Publisher

The following code example demonstrates an asynchronous Publisher that sends a finite sequence of events from Iterator. Its structure is similar to the synchronous Producer discussed earlier. The main difference is that the Publisher's _onSubscribe_ method and the Subscription’s _request_ and _cancel_ methods are not processed in the caller's thread, but in another thread in a given Executor.

<sub>The GitHub repository has unit tests to verify that this Publisher complies with all the specification rules that are checked in its TCK.</sub>


```java
public class AsyncIteratorPublisher<T> implements Flow.Publisher<T> {

   private final Supplier<Iterator<? extends T>> iteratorSupplier;
   private final Executor executor;
   private final int batchSize;

   public AsyncIteratorPublisher(Supplier<Iterator<? extends T>> iteratorSupplier, int batchSize, Executor executor) {
       if (batchSize < 1) {
           throw new IllegalArgumentException();
       }
       this.iteratorSupplier = Objects.requireNonNull(iteratorSupplier);
       this.executor = Objects.requireNonNull(executor);
       this.batchSize = batchSize;
   }

   @Override
   public void subscribe(Flow.Subscriber<? super T> subscriber) {
       // By rule 1.11, a Publisher may support multiple Subscribers and decide whether each Subscription is unicast or multicast.
       new SubscriptionImpl(subscriber).init();
   }

   private class SubscriptionImpl implements Flow.Subscription, Runnable {

       private final Flow.Subscriber<? super T> subscriber;

       private Iterator<? extends T> iterator;
       private long demand = 0;
       private boolean cancelled = false;

       SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
           // By rule 1.9, calling Publisher.subscribe must throw a NullPointerException when the given parameter is null.
           this.subscriber = Objects.requireNonNull(subscriber);
       }

       private void doSubscribe() {
           try {
               iterator = iteratorSupplier.get();
           } catch (Throwable throwable) {
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
               doError(throwable);
           }

           if (!cancelled) {
               subscriber.onSubscribe(this);

               boolean hasNext = false;
               try {
                   hasNext = iterator.hasNext();
               } catch (Throwable throwable) {
                   // By rule 1.4, if a Publisher fails it must signal an onError.
                   doError(throwable);
               }

               if (!hasNext) {
                   doCancel();
                   subscriber.onComplete();
               }
           }
       }

       private void doRequest(long n) {
           if (n <= 0) {
               // By rule 3.9, while the Subscription is not cancelled, Subscription.request(long) must signal onError with a IllegalArgumentException if the argument is <= 0.
               doError(new IllegalArgumentException("non-positive subscription request"));
           } else if (demand + n <= 0) {
               // By rule 3.17, a Subscription must support a demand up to Long.MAX_VALUE.
               demand = Long.MAX_VALUE;
               doNext();
           } else {
               // By rule 3.8, while the Subscription is not cancelled, Subscription.request(long) must register the given number of additional elements to be produced to the respective Subscriber.
               demand += n;
               doNext();
           }
       }

       // By rule 1.2, a Publisher may signal fewer onNext than requested and terminate the Subscription by calling onComplete or onError.
       private void doNext() {
           int batchLeft = batchSize;
           do {
               T next;
               boolean hasNext;
               try {
                   next = iterator.next();
                   hasNext = iterator.hasNext();
               } catch (Throwable throwable) {
                   // By rule 1.4, if a Publisher fails it must signal an onError.
                   doError(throwable);
                   return;
               }
               subscriber.onNext(next);

               if (!hasNext) {
                   // By rule 1.6, if a Publisher signals onComplete on a Subscriber, that Subscriber's Subscription must be considered cancelled.
                   doCancel();
                   // By rule 1.5, if a Publisher terminates successfully it must signal an onComplete.
                   subscriber.onComplete();
               }
           } while (!cancelled && --batchLeft > 0 && --demand > 0);

           if (!cancelled && demand > 0) {
               signal(new Next());
           }
       }

       private void doCancel() {
           logger.info("subscription.cancelled");
           cancelled = true;
       }

       private void doError(Throwable throwable) {
           // By rule 1.6, if a Publisher signals onError on a Subscriber, that Subscriber's Subscription must be considered cancelled.
           cancelled = true;
           subscriber.onError(throwable);
       }

       private void init() {
           signal(new Subscribe());
       }

       @Override
       public void request(long n) {
           logger.info("subscription.request: {}", n);
           signal(new Request(n));
       }

       @Override
       public void cancel() {
           logger.info("subscription.cancel");
           signal(new Cancel());
       }
```


The classes that implement the Signal interface represent asynchronous signals sent from the caller’s thread to the Executor’s worker thread. The Subscribe class is a signal for creation of a new subscription. The Request and Cancel classes are signals for handling Subscription’s _request_ and _cancel_ events. The Next class is a signal to asynchronous sending multiple Subscriber’s _onNext_ events during one asynchronous call (to avoid frequent context switching).


```java
       // These classes represent the asynchronous signals.
       private interface Signal extends Runnable {
       }

       private class Subscribe implements Signal {
           @Override
           public void run() {
               doSubscribe();
           }
       }

       private class Request implements Signal {
           private final long n;

           Request(long n) {
               this.n = n;
           }

           @Override
           public void run() {
               doRequest(n);
           }
       }

       private class Next implements Signal {
           @Override
           public void run() {
               doNext();
           }
       }

       private class Cancel implements Signal {
           @Override
           public void run() {
               doCancel();
           }
       }
```


The unbounded, thread-safe, non-blocking ConcurrentLinkedQueue queue transmits signals across threads. The AtomicBoolean mutex guarantees _serial_ processing of asynchronous signals in the Executor instance.


```java
       // The non-blocking queue to transmit signals in a thread-safe way.
       private final ConcurrentLinkedQueue<Signal> signalsQueue = new ConcurrentLinkedQueue<>();

       // The mutex to establish the happens-before relationship between asynchronous signal calls.
       private final AtomicBoolean mutex = new AtomicBoolean(false);

       private void signal(Signal signal) {
           logger.debug("signal.offer {}", signal);
           if (signalsQueue.offer(signal)) {
               tryExecute();
           }
       }

       @Override
       public void run() {
           // By rule 1.3, a Subscriber must ensure that all calls on its Subscriber's onSubscribe, onNext, onError, onComplete signaled to a Subscriber must be signaled serially.
           if (mutex.get()) {
               try {
                   Signal signal = signalsQueue.poll();
                   logger.debug("signal.poll {}", signal);
                   if (!cancelled) {
                       signal.run();
                   }
               } finally {
                   mutex.set(false);
                   if (!signalsQueue.isEmpty()) {
                       tryExecute();
                   }
               }
           }
       }

       private void tryExecute() {
           if (mutex.compareAndSet(false, true)) {
               try {
                   executor.execute(this);
               } catch (Throwable throwable) {
                   if (!cancelled) {
                       doCancel();
                       try {
                           // By rule 1.4, if a Publisher fails it must signal an onError.
                           doError(new IllegalStateException(throwable));
                       } finally {
                           signalsQueue.clear();
                           mutex.set(false);
                       }
                   }
               }
           }
       }
   }
}
```



### Asynchronous Subscriber

The following code sample demonstrates an asynchronous Subscriber that _pulls_ items one by one. Its structure is similar to the synchronous Subscriber discussed earlier. The main difference is that the Subscriber's _onSubscribe_, _onNext_, _onError_, _onComplete_ methods are not processed in the Publisher's thread, but in another thread in a given Executor.

<sub>The GitHub repository also has blackbox and whitebox unit tests to verify that this Subscriber meets the specification using its TCK.</sub>


```java
public class AsyncSubscriber<T> implements Flow.Subscriber<T>, Runnable {

   private final int id;
   private final CountDownLatch completed = new CountDownLatch(1);
   private final Executor executor;

   private Flow.Subscription subscription;
   private boolean cancelled = false;

   public AsyncSubscriber(int id, Executor executor) {
       this.id = id;
       this.executor = Objects.requireNonNull(executor);
   }

   @Override
   public void onSubscribe(Flow.Subscription subscription) {
       logger.info("({}) subscriber.subscribe: {}", id, subscription);
       // By rule 2.13, calling onSubscribe must throw a NullPointerException when the given parameter is null.
       signal(new OnSubscribe(Objects.requireNonNull(subscription)));
   }

   @Override
   public void onNext(T item) {
       logger.info("({}) subscriber.next: {}", id, item);
       // By rule 2.13, calling onNext must throw a NullPointerException when the given parameter is null.
       signal(new OnNext(Objects.requireNonNull(item)));
   }

   @Override
   public void onError(Throwable throwable) {
       logger.error("({}) subscriber.error", id, throwable);
       // By rule 2.13, calling onError must throw a NullPointerException when the given parameter is null.
       signal(new OnError(Objects.requireNonNull(throwable)));
   }

   @Override
   public void onComplete() {
       logger.info("({}) subscriber.complete", id);
       signal(new OnComplete());
   }

   public void awaitCompletion() throws InterruptedException {
       completed.await();
   }

   // This method is invoked when OnNext signals arrive and returns whether more elements are desired or not (is intended to override).
   protected boolean whenNext(T item) {
       return true;
   }

   // This method is invoked when an OnError signal arrives (is intended to override).
   protected void whenError(Throwable throwable) {
   }

   // This method is invoked when an OnComplete signal arrives (is intended to override).
   protected void whenComplete() {
       completed.countDown();
   }

   private void doSubscribe(Flow.Subscription subscription) {
       if (this.subscription != null) {
           // By rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription after an onSubscribe signal if it already has an active Subscription.
           subscription.cancel();
       } else {
           this.subscription = subscription;
           // By rule 2.1, a Subscriber must signal demand via Subscription.request(long) to receive onNext signals.
           this.subscription.request(1);
       }
   }

   private void doNext(T element) {
       // By rule 2.8, a Subscriber must be prepared to receive one or more onNext signals after having called Subscription.cancel()
       if (!cancelled) {
           if (whenNext(element)) {
               // By rule 2.1, a Subscriber must signal demand via Subscription.request(long) to receive onNext signals.
               subscription.request(1);
           } else {
               // By rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
               doCancel();
           }
       }
   }

   private void doError(Throwable throwable) {
       // By rule 2.4, Subscriber.onError(Throwable) must consider the Subscription cancelled after having received the signal.
       cancelled = true;
       whenError(throwable);
   }

   private void doComplete() {
       // By rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
       cancelled = true;
       whenComplete();
   }

   private void doCancel() {
       cancelled = true;
       subscription.cancel();
   }
```


The classes that implement the Signal interface represent asynchronous signals sent from the Publisher’s thread to the Executor’s worker thread. The OnSubscribe, OnNext, OnError, OnComplete classes are signals for handling the correspondent Subscriber’s events.


```java
   // These classes represent the asynchronous signals.
   private interface Signal extends Runnable {
   }

   private class OnSubscribe implements Signal {
       private final Flow.Subscription subscription;

       OnSubscribe(Flow.Subscription subscription) {
           this.subscription = subscription;
       }

       @Override
       public void run() {
           doSubscribe(subscription);
       }
   }

   private class OnNext implements Signal {
       private final T element;

       OnNext(T element) {
           this.element = element;
       }

       @Override
       public void run() {
           doNext(element);
       }
   }

   private class OnError implements Signal {
       private final Throwable throwable;

       OnError(Throwable throwable) {
           this.throwable = throwable;
       }

       @Override
       public void run() {
           doError(throwable);
       }
   }

   private class OnComplete implements Signal {
       @Override
       public void run() {
           doComplete();
       }
   }
```


The asynchronous Consumer uses the same ConcurrentLinkedQueue queue and AtomicBoolean mutex to _serially_ process asynchronous signals in the Executor instance.


```java
   // The non-blocking queue to transmit signals in a thread-safe way.
   private final ConcurrentLinkedQueue<Signal> signalsQueue = new ConcurrentLinkedQueue<>();

   // The mutex to establish the happens-before relationship between asynchronous signal calls.
   private final AtomicBoolean mutex = new AtomicBoolean(false);

   @Override
   public void run() {
       // By rule 2.7, a Subscriber must ensure that all calls on its Subscription's request, cancel methods are performed serially.
       if (mutex.get()) {
           try {
               Signal signal = signalsQueue.poll();
               logger.debug("({}) signal.poll {}", id, signal);
               if (!cancelled) {
                   signal.run();
               }
           } finally {
               mutex.set(false);
               if (!signalsQueue.isEmpty()) {
                   tryExecute();
               }
           }
       }
   }

   private void signal(Signal signal) {
       logger.debug("({}) signal.offer {}", id, signal);
       if (signalsQueue.offer(signal)) {
           tryExecute();
       }
   }

   private void tryExecute() {
       if (mutex.compareAndSet(false, true)) {
           try {
               executor.execute(this);
           } catch (Throwable throwable) {
               if (!cancelled) {
                   try {
                       doCancel();
                   } finally {
                       signalsQueue.clear();
                       mutex.set(false);
                   }
               }
           }
       }
   }
}
```



### Asynchronous reactive stream

The following code example demonstrates that the _multicast_ asynchronous Publisher sends the same sequence of events (the same pangram) to two asynchronous Subscribers.


```java
ExecutorService executorService = Executors.newFixedThreadPool(3);

List<String> words = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
AsyncIteratorPublisher<String> publisher = new AsyncIteratorPublisher<>(() -> List.copyOf(words).iterator(), 1024, executorService);

AsyncSubscriber<String> subscriber1 = new AsyncSubscriber<>(1, executorService);
publisher.subscribe(subscriber1);

AsyncSubscriber<String> subscriber2 = new AsyncSubscriber<>(2, executorService);
publisher.subscribe(subscriber2);

subscriber1.awaitCompletion();
subscriber2.awaitCompletion();

executorService.shutdown();
executorService.awaitTermination(60, TimeUnit.SECONDS);
```


The following log shows that the asynchronous Publisher sends the sequence of events in a separate thread, and the asynchronous Subscribers receive the sequence of events also in a separate thread _at the same time_.


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
