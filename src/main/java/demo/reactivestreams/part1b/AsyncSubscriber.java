/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package demo.reactivestreams.part1b;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AsyncSubscriber is an implementation of Reactive Streams `Subscriber`,
 * it runs asynchronously (on an Executor), requests one element
 * at a time, and invokes a user-defined method to process each element.
 *
 * NOTE: The code below uses a lot of try-catches to show the reader where exceptions can be expected, and where they are forbidden.
 */
public abstract class AsyncSubscriber<T> implements Flow.Subscriber<T>, Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AsyncSubscriber.class);

  private static interface Signal {
  }

  private static class OnSubscribe implements Signal {
    public final Flow.Subscription subscription;

    public OnSubscribe(final Flow.Subscription subscription) {
      this.subscription = subscription;
    }
  }

  private static class OnNext<T> implements Signal {
    public final T next;

    public OnNext(final T next) {
      this.next = next;
    }
  }

  private static class OnError implements Signal {
    public final Throwable error;

    public OnError(final Throwable error) {
      this.error = error;
    }
  }

  private enum OnComplete implements Signal {Instance;}

  private Flow.Subscription subscription; // Obeying rule 3.1, we make this private!
  private boolean done; // It's useful to keep track of whether this Subscriber is done or not

  private final Executor executor;

  protected AsyncSubscriber(Executor executor) {
    if (executor == null) {
      throw new NullPointerException();
    }
    this.executor = executor;
  }

  // Showcases a convenience method to idempotently marking the Subscriber as "done", so we don't want to process more elements
  // herefor we also need to cancel our `Subscription`.
  private final void done() {
    //On this line we could add a guard against `!done`, but since rule 3.7 says that `Subscription.cancel()` is idempotent, we don't need to.
    done = true; // If `whenNext` throws an exception, let's consider ourselves done (not accepting more elements)
    if (subscription != null) { // If we are bailing out before we got a `Subscription` there's little need for cancelling it.
      try {
        subscription.cancel(); // Cancel the subscription
      } catch(final Throwable t) {
        //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
        (new IllegalStateException(subscription + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
      }
    }
  }

  protected abstract boolean whenNext(T element);

  protected void whenComplete() {
  }

  protected void whenError(Throwable throwable) {
  }

  private final void handleOnSubscribe(final Flow.Subscription s) {
    if (s == null) {
      // Getting a null `Subscription` here is not valid so lets just ignore it.
    } else if (subscription != null) { // If someone has made a mistake and added this Subscriber multiple times, let's handle it gracefully
      try {
        s.cancel(); // Cancel the additional subscription to follow rule 2.5
      } catch(final Throwable t) {
        //Subscription.cancel is not allowed to throw an exception, according to rule 3.15
        (new IllegalStateException(s + " violated the Reactive Streams rule 3.15 by throwing an exception from cancel.", t)).printStackTrace(System.err);
      }
    } else {
      // We have to assign it locally before we use it, if we want to be a synchronous `Subscriber`
      // Because according to rule 3.10, the Subscription is allowed to call `onNext` synchronously from within `request`
      subscription = s;
      try {
        // If we want elements, according to rule 2.1 we need to call `request`
        // And, according to rule 3.2 we are allowed to call this synchronously from within the `onSubscribe` method
        s.request(1); // Our Subscriber is unbuffered and modest, it requests one element at a time
      } catch(final Throwable t) {
        // Subscription.request is not allowed to throw according to rule 3.16
        (new IllegalStateException(s + " violated the Reactive Streams rule 3.16 by throwing an exception from request.", t)).printStackTrace(System.err);
      }
    }
  }

  private void handleOnNext(T element) {
    if (!done) {
      if (whenNext(element)) {
        subscription.request(1);
      } else {
        done();
      }
    }
  }

  private void handleOnError(Throwable throwable) {
    done = true;
    whenError(throwable);
  }

  private void handleOnComplete() {
    done = true;
    whenComplete();
  }

  @Override
  public  void onSubscribe(Flow.Subscription s) {
    logger.info("subscriber.subscribe: {}", s);
    if (s == null) {
      throw new NullPointerException();
    }

    signal(new OnSubscribe(s));
  }

  @Override
  public  void onNext( T element) {
    logger.info("subscriber.next: {}", element);
    if (element == null) {
      throw new NullPointerException();
    }

    signal(new OnNext<T>(element));
  }

  @Override
  public  void onError( Throwable throwable) {
    logger.error("subscriber.error", throwable);
    if (throwable == null) {
      throw new NullPointerException();
    }

    signal(new OnError(throwable));
  }

  @Override
  public  void onComplete() {
    logger.info("subscriber.complete");
    signal(OnComplete.Instance);
  }

  // This `ConcurrentLinkedQueue` will track signals that are sent to this `Subscriber`, like `OnComplete` and `OnNext` ,
  // and obeying rule 2.11
  private final ConcurrentLinkedQueue<Signal> inboundSignals = new ConcurrentLinkedQueue<Signal>();

  // We are using this `AtomicBoolean` to make sure that this `Subscriber` doesn't run concurrently with itself,
  // obeying rule 2.7 and 2.11
  private final AtomicBoolean on = new AtomicBoolean(false);

   @SuppressWarnings("unchecked")
   @Override public final void run() {
    if(on.get()) { // establishes a happens-before relationship with the end of the previous run
      try {
        final Signal s = inboundSignals.poll(); // We take a signal off the queue
        if (!done) { // If we're done, we shouldn't process any more signals, obeying rule 2.8
          // Below we simply unpack the `Signal`s and invoke the corresponding methods
          if (s instanceof OnNext<?>)
            handleOnNext(((OnNext<T>)s).next);
          else if (s instanceof OnSubscribe)
            handleOnSubscribe(((OnSubscribe)s).subscription);
          else if (s instanceof OnError) // We are always able to handle OnError, obeying rule 2.10
            handleOnError(((OnError)s).error);
          else if (s == OnComplete.Instance) // We are always able to handle OnComplete, obeying rule 2.9
            handleOnComplete();
        }
      } finally {
        on.set(false); // establishes a happens-before relationship with the beginning of the next run
        if(!inboundSignals.isEmpty()) // If we still have signals to process
          tryScheduleToExecute(); // Then we try to schedule ourselves to execute again
      }
    }
  }

  // What `signal` does is that it sends signals to the `Subscription` asynchronously
  private void signal(final Signal signal) {
    if (inboundSignals.offer(signal)) // No need to null-check here as ConcurrentLinkedQueue does this for us
      tryScheduleToExecute(); // Then we try to schedule it for execution, if it isn't already
  }

  // This method makes sure that this `Subscriber` is only executing on one Thread at a time
  private final void tryScheduleToExecute() {
    if(on.compareAndSet(false, true)) {
      try {
        executor.execute(this);
      } catch(Throwable t) { // If we can't run on the `Executor`, we need to fail gracefully and not violate rule 2.13
        if (!done) {
          try {
            done(); // First of all, this failure is not recoverable, so we need to cancel our subscription
          } finally {
            inboundSignals.clear(); // We're not going to need these anymore
            // This subscription is cancelled by now, but letting the Subscriber become schedulable again means
            // that we can drain the inboundSignals queue if anything arrives after clearing
            on.set(false);
          }
        }
      }
    }
  }
}
