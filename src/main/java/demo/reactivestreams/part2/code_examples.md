# Reactive Streams specification in Java


## Code examples


### Cold synchronous reactive stream

The following class demonstrates a synchronous Publisher that sends a finite sequence of events from an Iterator. The _synchronous_ Publisher processes its _subscribe_ event, and the Subscription events _request_ and _cancel_ in the caller thread. This Publisher is _multicast_, and can send events to multiple Subscribers, storing information about each connection in a nested private implementation of the Subscription interface. The Subscription implementation contains the current Iterator instance, the demand, and the cancellation flag. To make a _cold_ Publisher that sends the same sequence of events for each Subscriber, the Publisher stores a Supplier that must return a new Iterator instance for each invocation. The Publisher uses different types of error handling (throwing an exception or calling the Subscriber _onError_ method) according to the Reactive Streams specification.


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

The following code fragment demonstrates that this _multicast_ synchronous Publisher sends the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to these two synchronous Subscribers.


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


The invocation log of this fragment shows that the synchronous Publisher sends a sequence of events in the caller thread, and the synchronous Subscribers receive a sequence of events in the Publisher thread (the same caller thread) _one at a time_.


```
16:49:28.973  main                              (1) subscriber.subscribe: demo.reactivestreams.part1.SyncIteratorPublisher$SubscriptionImpl@7791a895
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: The
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: quick
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: brown
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: fox
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: jumps
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: over
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: the
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: lazy
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.next: dog.
16:49:28.976  main                              subscription.request: 1
16:49:28.976  main                              (1) subscriber.complete
16:49:28.976  main                              (2) subscriber.subscribe: demo.reactivestreams.part1.SyncIteratorPublisher$SubscriptionImpl@610694f1
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: The
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: quick
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: brown
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: fox
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: jumps
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: over
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: the
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: lazy
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.next: dog.
16:49:28.977  main                              subscription.request: 1
16:49:28.977  main                              (2) subscriber.complete
```



### Cold asynchronous reactive stream

The following class demonstrates an asynchronous Publisher that also sends a finite sequence of events from an Iterator. Its structure is similar to the synchronous Producer mentioned earlier. The main difference is that the Publisher event _onSubscribe_ and the Subscription events _request_ and _cancel_ are processed not in the caller thread but in another worker thread of the given Executor instance.


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
       // By rule 1.11, a Publisher may support multiple Subscribers and decide
       // whether each Subscription is unicast or multicast.
       new SubscriptionImpl(subscriber).init();
   }

   private class SubscriptionImpl implements Flow.Subscription, Runnable {

       private final Flow.Subscriber<? super T> subscriber;
       private final AtomicLong demand = new AtomicLong(0);
       private final AtomicBoolean cancelled = new AtomicBoolean(false);

       private Iterator<? extends T> iterator;

       SubscriptionImpl(Flow.Subscriber<? super T> subscriber) {
           // By rule 1.9, calling Publisher.subscribe(Subscriber)
           // must throw a NullPointerException when the given parameter is null.
           this.subscriber = Objects.requireNonNull(subscriber);
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

       private void doSubscribe() {
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

               boolean hasNext = false;
               try {
                   hasNext = iterator.hasNext();
               } catch (Throwable t) {
                   // By rule 1.4, if a Publisher fails it must signal an onError.
                   doError(t);
               }

               if (!hasNext) {
                   doCancel();
                   subscriber.onComplete();
               }
           }
       }

       private void doRequest(long n) {
           if (n <= 0) {
               // By rule 3.9, while the Subscription is not cancelled, Subscription.request(long)
               // must signal onError with a IllegalArgumentException if the argument is <= 0.
               doError(new IllegalArgumentException("non-positive subscription request"));
           } else {
               if (demand.get() == Long.MAX_VALUE) {
                   // By rule 3.17, a demand equal or greater than Long.MAX_VALUE
                   // may be considered by the Publisher as "effectively unbounded".
                   return;
               }

               if (demand.get() + n <= 0) {
                   // By rule 3.17, a Subscription must support a demand up to Long.MAX_VALUE.
                   demand.set(Long.MAX_VALUE);
               } else {
                   // By rule 3.8, while the Subscription is not cancelled, Subscription.request(long)
                   // must register the given number of additional elements to be produced to the respective Subscriber.
                   demand.addAndGet(n);
               }

               doNext();
           }
       }

       // By rule 1.2, a Publisher may signal fewer onNext than requested
       // and terminate the Subscription by calling onComplete or onError.
       private void doNext() {
           int batchLeft = batchSize;
           do {
               T next;
               boolean hasNext;
               try {
                   next = iterator.next();
                   hasNext = iterator.hasNext();
               } catch (Throwable t) {
                   // By rule 1.4, if a Publisher fails it must signal an onError.
                   doError(t);
                   return;
               }
               subscriber.onNext(next);

               if (!hasNext) {
                   // By rule 1.6, if a Publisher signals onComplete on a Subscriber,
                   // that Subscriber's Subscription must be considered cancelled.
                   doCancel();
                   // By rule 1.5, if a Publisher terminates successfully it must signal an onComplete.
                   subscriber.onComplete();
               }
           } while (!cancelled.get() && --batchLeft > 0 && demand.decrementAndGet() > 0);

           if (!cancelled.get() && demand.get() > 0) {
               signal(new Next());
           }
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
```


The classes that implement the Signal interface represent asynchronous signals sent from the caller thread to the Executor worker thread. The Subscribe class is a signal to create a new subscription. The Request and Cancel classes are signals for processing the Subscription events _request_ and _cancel_. The Next class is a signal to send multiple Subscriber _onNext_ events during a single asynchronous call.


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


The unbounded, thread-safe, non-blocking ConcurrentLinkedQueue queue transmits the signals between threads. The AtomicBoolean mutex ensures that these asynchronous signals are processed _serially_ in the Executor instance.


```java
       // The non-blocking queue to transmit signals in a thread-safe way.
       private final Queue<Signal> signalsQueue = new ConcurrentLinkedQueue<>();

       // The mutex to establish the happens-before relationship between asynchronous signal calls.
       private final AtomicBoolean mutex = new AtomicBoolean(false);

       private void signal(Signal signal) {
           if (signalsQueue.offer(signal)) {
               tryExecute();
           }
       }

       @Override
       public void run() {
           // By rule 1.3, a Subscriber must ensure that all calls on its Subscriber's
           // onSubscribe, onNext, onError, onComplete signaled to a Subscriber must be signaled serially.
           if (mutex.get()) {
               try {
                   Signal signal = signalsQueue.poll();
                   if (!cancelled.get()) {
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
               } catch (Throwable t) {
                   if (!cancelled.get()) {
                       doCancel();
                       try {
                           // By rule 1.4, if a Publisher fails it must signal an onError.
                           doError(new IllegalStateException(t));
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


The following code sample demonstrates an asynchronous Subscriber that also _pulls_ items one by one. Its structure is similar to the synchronous Subscriber mentioned earlier. The main difference is that the Subscriber events _onSubscribe_, _onNext_, _onError_, _onComplete_ are processed not in the Publisher thread but in another worker thread of the given Executor instance.


```java
public class AsyncSubscriber<T> implements Flow.Subscriber<T>, Runnable {

   private static final Logger logger = LoggerFactory.getLogger(AsyncSubscriber.class);

   private final int id;
   private final AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();
   private final AtomicBoolean cancelled = new AtomicBoolean(false);
   private final CountDownLatch completed = new CountDownLatch(1);
   private final Executor executor;

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
   public void onError(Throwable t) {
       logger.error("({}) subscriber.error", id, t);
       // By rule 2.13, calling onError must throw a NullPointerException when the given parameter is null.
       signal(new OnError(Objects.requireNonNull(t)));
   }

   @Override
   public void onComplete() {
       logger.info("({}) subscriber.complete", id);
       signal(new OnComplete());
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

   private void doSubscribe(Flow.Subscription subscription) {
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

   private void doNext(T element) {
       // By rule 2.8, a Subscriber must be prepared to receive one or more onNext signals
       // after having called Subscription.cancel()
       if (!cancelled.get()) {
           if (whenNext(element)) {
               // By rule 2.1, a Subscriber must signal demand via Subscription.request(long) to receive onNext signals.
               subscription.get().request(1);
           } else {
               // By rule 2.6, a Subscriber must call Subscription.cancel() if the Subscription is no longer needed.
               doCancel();
           }
       }
   }

   private void doError(Throwable t) {
       // By rule 2.4, Subscriber.onError(Throwable) must consider the Subscription cancelled
       // after having received the signal.
       cancelled.set(true);
       whenError(t);
   }

   private void doComplete() {
       // By rule 2.4, Subscriber.onComplete() must consider the Subscription cancelled
       // after having received the signal.
       cancelled.set(true);
       whenComplete();
   }

   private void doCancel() {
       cancelled.set(true);
       subscription.get().cancel();
   }
```


The classes that implement the Signal interface represent asynchronous signals sent from the Publisher thread to the Executor worker thread. The OnSubscribe, OnNext, OnError, OnComplete classes are signals for processing the correspondent Subscriber events.


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
       private final Throwable t;

       OnError(Throwable t) {
           this.t = t;
       }

       @Override
       public void run() {
           doError(t);
       }
   }

   private class OnComplete implements Signal {
       @Override
       public void run() {
           doComplete();
       }
   }
```


The asynchronous Consumer uses the same ConcurrentLinkedQueue queue and AtomicBoolean mutex to process these asynchronous signals _serially_ in the Executor instance.


```java
   // The non-blocking queue to transmit signals in a thread-safe way.
   private final Queue<Signal> signalsQueue = new ConcurrentLinkedQueue<>();

   // The mutex to establish the happens-before relationship between asynchronous signal calls.
   private final AtomicBoolean mutex = new AtomicBoolean(false);

   @Override
   public void run() {
       // By rule 2.7, a Subscriber must ensure that all calls on its Subscription's request, cancel methods
       // are performed serially.
       if (mutex.get()) {
           try {
               Signal signal = signalsQueue.poll();
               if (!cancelled.get()) {
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
       if (signalsQueue.offer(signal)) {
           tryExecute();
       }
   }

   private void tryExecute() {
       if (mutex.compareAndSet(false, true)) {
           try {
               executor.execute(this);
           } catch (Throwable t) {
               if (!cancelled.get()) {
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


<sub>The GitHub repository has <a href="https://github.com/aliakh/demo-java-reactive-streams/tree/main/src/test/java/demo/reactivestreams/part2">unit tests</a> to verify that the Publisher and Subscriber comply with all the Reactive Streams specification contracts that its TCK checks.</sub>

The following code fragment demonstrates that this _multicast_ asynchronous Publisher sends the same sequence of events (the same pangram) to these two asynchronous Subscribers.


```java
ExecutorService executorService = Executors.newFixedThreadPool(3);

List<String> words = List.of("The quick brown fox jumps over the lazy dog.".split(" "));
Supplier<Iterator<? extends String>> iteratorSupplier = () -> List.copyOf(words).iterator();
AsyncIteratorPublisher<String> publisher = new AsyncIteratorPublisher<>(iteratorSupplier, 128, executorService);

AsyncSubscriber<String> subscriber1 = new AsyncSubscriber<>(1, executorService);
publisher.subscribe(subscriber1);

AsyncSubscriber<String> subscriber2 = new AsyncSubscriber<>(2, executorService);
publisher.subscribe(subscriber2);

subscriber1.awaitCompletion();
subscriber2.awaitCompletion();

executorService.shutdown();
executorService.awaitTermination(60, TimeUnit.SECONDS);
```


The invocation log of this fragment shows that the asynchronous Publisher sends a sequence of events in a separate thread, and the asynchronous Subscribers receive a sequence of events in other separate threads _at the same time_.


```
16:49:28.982  pool-1-thread-1                   (1) subscriber.subscribe: demo.reactivestreams.part2.AsyncIteratorPublisher$SubscriptionImpl@8cc3a44
16:49:28.982  pool-1-thread-2                   (2) subscriber.subscribe: demo.reactivestreams.part2.AsyncIteratorPublisher$SubscriptionImpl@454972b8
16:49:28.982  pool-1-thread-1                   subscription.request: 1
16:49:28.983  pool-1-thread-3                   subscription.request: 1
16:49:28.983  pool-1-thread-1                   (1) subscriber.next: The
16:49:28.983  pool-1-thread-3                   (2) subscriber.next: The
16:49:28.983  pool-1-thread-1                   subscription.request: 1
16:49:28.983  pool-1-thread-3                   subscription.request: 1
16:49:28.983  pool-1-thread-1                   (1) subscriber.next: quick
16:49:28.983  pool-1-thread-2                   (2) subscriber.next: quick
16:49:28.983  pool-1-thread-1                   subscription.request: 1
16:49:28.983  pool-1-thread-1                   subscription.request: 1
16:49:28.983  pool-1-thread-2                   (1) subscriber.next: brown
16:49:28.983  pool-1-thread-1                   (2) subscriber.next: brown
16:49:28.983  pool-1-thread-2                   subscription.request: 1
16:49:28.983  pool-1-thread-1                   subscription.request: 1
16:49:28.983  pool-1-thread-1                   (1) subscriber.next: fox
16:49:28.983  pool-1-thread-2                   (2) subscriber.next: fox
16:49:28.983  pool-1-thread-1                   subscription.request: 1
16:49:28.983  pool-1-thread-2                   subscription.request: 1
16:49:28.983  pool-1-thread-1                   (1) subscriber.next: jumps
16:49:28.983  pool-1-thread-2                   (2) subscriber.next: jumps
16:49:28.983  pool-1-thread-1                   subscription.request: 1
16:49:28.983  pool-1-thread-2                   subscription.request: 1
16:49:28.983  pool-1-thread-1                   (1) subscriber.next: over
16:49:28.984  pool-1-thread-2                   (2) subscriber.next: over
16:49:28.984  pool-1-thread-1                   subscription.request: 1
16:49:28.984  pool-1-thread-2                   subscription.request: 1
16:49:28.984  pool-1-thread-1                   (1) subscriber.next: the
16:49:28.984  pool-1-thread-2                   (2) subscriber.next: the
16:49:28.984  pool-1-thread-1                   subscription.request: 1
16:49:28.984  pool-1-thread-3                   subscription.request: 1
16:49:28.984  pool-1-thread-1                   (1) subscriber.next: lazy
16:49:28.984  pool-1-thread-3                   subscription.request: 1
16:49:28.984  pool-1-thread-2                   (2) subscriber.next: lazy
16:49:28.984  pool-1-thread-2                   subscription.request: 1
16:49:28.984  pool-1-thread-3                   (1) subscriber.next: dog.
16:49:28.984  pool-1-thread-2                   (2) subscriber.next: dog.
16:49:28.984  pool-1-thread-3                   (1) subscriber.complete
16:49:28.984  pool-1-thread-2                   (2) subscriber.complete
16:49:28.984  pool-1-thread-1                   subscription.request: 1
16:49:28.984  pool-1-thread-1                   subscription.request: 1
```



### Hot asynchronous reactive stream

The following class demonstrates an asynchronous Publisher that sends an infinite sequence of events from a [WatchService](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/WatchService.html) implementation for a file system. The _asynchronous_ Publisher is inherited from the [SubmissionPublisher](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/SubmissionPublisher.html) class and reuses its Executor for asynchronous invocations. This Publisher is _hot_ and sends "live" events about file changes in the given folder.


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


The following class demonstrates an asynchronous Processor that filters events (by the given file extension) and transforms them (from WatchEvent&lt;Path> to String). As a Subscriber, this Processor implements the Subscriber methods _onSubscribe_, _onNext_, _onError_, _onComplete_ to connect to an upstream Producer and _pull_ items one by one. As a Publisher, this Processor uses the SubmissionPublisher _submit_ method to send items to a downstream Subscriber.


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


The invocation log of this fragment shows that the Publisher and the Processor use the internal implementation of the Subscription interface, the nested package-private SubmissionPublisher.BufferedSubscription class. These classes process events asynchronously by default in the worker threads of the common Fork/Join thread pool.


```
16:49:28.988  ForkJoinPool.commonPool-worker-2  subscriber.subscribe: java.util.concurrent.SubmissionPublisher$BufferedSubscription@783e9d8b
16:49:28.988  ForkJoinPool.commonPool-worker-3  processor.subscribe: java.util.concurrent.SubmissionPublisher$BufferedSubscription@27e4c4b3
16:49:45.138  ForkJoinPool.commonPool-worker-1  publisher.submit: path example.txt, action ENTRY_CREATE
16:49:45.139  ForkJoinPool.commonPool-worker-4  processor.next: path example.txt, action ENTRY_CREATE
16:49:45.140  ForkJoinPool.commonPool-worker-4  subscriber.next: file example.txt is created
16:49:55.149  ForkJoinPool.commonPool-worker-1  publisher.submit: path example.txt, action ENTRY_MODIFY
16:49:55.149  ForkJoinPool.commonPool-worker-4  processor.next: path example.txt, action ENTRY_MODIFY
16:49:55.149  ForkJoinPool.commonPool-worker-4  subscriber.next: file example.txt is modified
16:50:05.151  ForkJoinPool.commonPool-worker-1  publisher.submit: path example.txt, action ENTRY_DELETE
16:50:05.151  ForkJoinPool.commonPool-worker-4  processor.next: path example.txt, action ENTRY_DELETE
16:50:05.151  ForkJoinPool.commonPool-worker-4  subscriber.next: file example.txt is deleted
16:50:28.989  main                              publisher.close
16:50:28.989  ForkJoinPool.commonPool-worker-7  processor.completed
16:50:28.989  ForkJoinPool.commonPool-worker-7  subscriber.complete
16:50:28.989  main                              publisher.close
```
