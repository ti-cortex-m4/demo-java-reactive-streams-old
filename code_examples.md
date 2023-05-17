# Reactive Streams specification in Java


## Code examples


### Cold synchronous reactive stream

The following class demonstrates a synchronous Publisher that sends a finite sequence of events from an Iterator. The _synchronous_ Publisher processes  its _subscribe_ method and the Subscription’s _request_ and _cancel_ methods in the caller’s thread. The _multicast_ Publisher can send items to multiple Subscribers, storing information about each connection in a private implementation of the Subscription interface. It includes the current Iterator instance, the demand (the aggregated number of items requested by a Subscriber which is yet to be delivered by the Publisher), and the connection cancellation flag. To make a _cold_ Publisher that sends the same sequence of events for each Subscriber, the Publisher stores a Supplier that must return a new Iterator instance for each new Subscription. The Publisher uses different types of error handling (throwing an exception or calling the _onError_ method) according to the specification.

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


The following class demonstrates a synchronous Subscriber that _pulls_ items one by one. The _synchronous_ Subscriber processes its _onSubscribe_, _onNext_, _onError_, _onComplete_ methods in the Publisher’s thread. The Subscriber also stores its Subscription (to perform backpressure) and its cancellation flag. The Subscriber also uses different types of error handling (throwing an exception or unsubscribing) according to the specification.

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


The following code fragment demonstrates that this _multicast_ synchronous Publisher sends the same sequence of events (_[The quick brown fox jumps over the lazy dog](https://en.wikipedia.org/wiki/The_quick_brown_fox_jumps_over_the_lazy_dog)_ pangram) to these two synchronous Subscribers.


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


Log from the invocation of the previous code fragment demonstrates that the synchronous Publisher sends the sequence of events in the caller's thread, and the synchronous Subscribers receive the sequence of events in the Publisher's thread (the same caller's thread) _one at a time_.


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
