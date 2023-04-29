## Code examples


### Synchronous publisher and subscriber

The following code example demonstrates a synchronous _cold_ Publisher that sends a finite sequence of items from a given Iterator.

The following code sample demonstrates a synchronous Subscriber that _pulls_ items one by one and logs received events. The comments show which code fragments are responsible for implementing which rules of the Reactive Streams specification. This synchronous Subscriber executes its methods onSubscribe, onNext, onError, onComplete in a Publisher’s thread. The GitHub repository also has blackbox and whitebox unit tests to verify that this Subscriber meets the specification using its TCK.


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
       // By rule 2.13, calling onSubscribe must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(subscription);

       if (this.subscription != null) {
           // By rule 2.5, a Subscriber must call Subscription.cancel() on the given Subscription after an onSubscribe signal if it already has an active Subscription.
           subscription.cancel();
       } else {
           this.subscription = subscription;
           // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
           this.subscription.request(1);
       }
   }

   @Override
   public void onNext(T item) {
       logger.info("({}) subscriber.next: {}", id, item);
       // By rule 2.13, calling onNext must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(item);

       // By rule 2.8, a Subscriber must be prepared to receive one or more onNext signals after having called Subscription.cancel()
       if (!cancelled) {
           if (whenNext(item)) {
               // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
               subscription.request(1);
           } else {
               // By rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
               doCancel();
           }
       }
   }

   @Override
   public void onError(Throwable throwable) {
       logger.error("({}) subscriber.error", id, throwable);
       // By rule 2.13, calling onError must throw a NullPointerException when the given parameter is null.
       Objects.requireNonNull(throwable);

       // By rule 2.4, Subscriber.onError(Throwable t) must consider the Subscription cancelled after having received the signal.
       cancelled = true;
       whenError(throwable);
   }

   @Override
   public void onComplete() {
       logger.info("({}) subscriber.complete", id);

       // By rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled after having received the signal.
       cancelled = true;
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
   protected void whenError(Throwable throwable) {
   }

   // This method is invoked when an OnComplete signal arrives (is intended to override).
   protected void whenComplete() {
       completed.countDown();
   }

   private void doCancel() {
       cancelled = true;
       subscription.cancel();
   }
}
```


The following code example demonstrates that the _multicast_ Publisher sends the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to multiple Subscribers.


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


The following log demonstrates that the synchronous Publisher sends a sequence of events in the caller's thread, and the synchronous Subscribers receive the sequence of events in the Publisher's thread _in sequence_.


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

The following code example demonstrates an asynchronous _cold_ Publisher that sends a finite sequence of items from a given Iterator.

The following code sample demonstrates an asynchronous Subscriber that _pulls_ items one by one and logs received events. The comments show which code fragments are responsible for implementing which rules of the Reactive Streams specification. This asynchronous Subscriber executes its methods onSubscribe, onNext, onError, onComplete in a separate thread. The thread-safe, non-blocking ConcurrentLinkedQueue transfers the signals from the Publisher’s thread to the Subscriber’s thread. The AtomicBoolean mutex ensures that the signals are executed _serially_ even if they are executed asynchronously. The GitHub repository also has blackbox and whitebox unit tests to verify that this Subscriber meets the specification using its TCK.


```
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
           // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
           this.subscription.request(1);
       }
   }

   private void doNext(T element) {
       // By rule 2.8, a Subscriber must be prepared to receive one or more onNext signals after having called Subscription.cancel()
       if (!cancelled) {
           if (whenNext(element)) {
               // By rule 2.1, a Subscriber must signal demand via Subscription.request(long n) to receive onNext signals.
               subscription.request(1);
           } else {
               // By rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
               doCancel();
           }
       }
   }

   private void doError(Throwable throwable) {
       // By rule 2.4, Subscriber.onError(Throwable t) must consider the Subscription cancelled after having received the signal.
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





```
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





```
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


The following code example demonstrates that the _multicast_ Publisher sends the same sequence of events (the same pangram) to multiple Subscribers.


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


The following log shows that an asynchronous Publisher sends a sequence of events in a separate thread, and that asynchronous Subscribers receive a sequence of events in a separate thread _in parallel_.


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


The GitHub repository also has examples of how a synchronous Publisher sends events to asynchronous Subscribers and how an asynchronous Publisher sends events to synchronous Subscribers.
