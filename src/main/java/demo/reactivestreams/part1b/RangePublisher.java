package demo.reactivestreams.part1b;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

public class RangePublisher implements Flow.Publisher<Integer> {

    /**
     * The starting value of the range.
     */
    final int start;

    /**
     * The number of items to emit.
     */
    final int count;

    /**
     * Constructs a RangePublisher instance with the given start and count values
     * that yields a sequence of [start, start + count).
     *
     * @param start the starting value of the range
     * @param count the number of items to emit
     */
    public RangePublisher(int start, int count) {
        this.start = start;
        this.count = count;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException();
        }

        subscriber.onSubscribe(new RangeSubscription(subscriber, start, start + count));
    }

    /**
     * A Subscription implementation that holds the current downstream
     * requested amount and responds to the downstream's request() and
     * cancel() calls.
     */
    static final class RangeSubscription
        // We are using this `AtomicLong` to make sure that this `Subscription`
        // doesn't run concurrently with itself, which would violate rule 1.3
        // among others (no concurrent notifications).
        // The atomic transition from 0L to N > 0L will ensure this.
        /*extends AtomicLong*/ implements Flow.Subscription {

        private final AtomicLong x = new AtomicLong();

        final Flow.Subscriber<? super Integer> subscriber;

        /**
         * The end index (exclusive).
         */
        final int end;

        /**
         * The current index and within the [start, start + count) range that
         * will be emitted as downstream.onNext().
         */
        int index;

        volatile boolean cancelled;

        /**
         * Holds onto the IllegalArgumentException (containing the offending stacktrace)
         * indicating there was a non-positive request() call from the downstream.
         */
        volatile Throwable error;

        RangeSubscription(Flow.Subscriber<? super Integer> subscriber, int start, int end) {
            this.subscriber = subscriber;
            this.index = start;
            this.end = end;
        }

        @Override
        public void request(long n) {
            // Non-positive requests should be honored with IllegalArgumentException
            if (n <= 0L) {
                error = new IllegalArgumentException("§3.9: non-positive requests are not allowed!");
                n = 1;
            }
            // Downstream requests are cumulative and may come from any thread
            for (; ; ) {
                long requested = x.get();
                long update = requested + n;
                // As governed by rule 3.17, when demand overflows `Long.MAX_VALUE`
                // we treat the signalled demand as "effectively unbounded"
                if (update < 0L) {
                    update = Long.MAX_VALUE;
                }
                // atomically update the current requested amount
                if (x.compareAndSet(requested, update)) {
                    // if there was no prior request amount, we start the emission loop
                    if (requested == 0L) {
                        emit(update);
                    }
                    break;
                }
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        void emit(long currentRequested) {
            // Load fields to avoid re-reading them from memory due to volatile accesses in the loop.
            int index = this.index;
            int end = this.end;
            int emitted = 0;

            for (; ; ) {
                // Check if there was an invalid request and then report its exception
                // as mandated by rule 3.9. The stacktrace in it should
                // help locate the faulty logic in the Subscriber.
                if (this.error != null) {
                    // When we signal onError, the subscription must be considered as cancelled, as per rule 1.6
                    cancelled = true;

                    this.subscriber.onError(this.error);
                    return;
                }

                // Loop while the index hasn't reached the end and we haven't
                // emitted all that's been requested
                while (index != end && emitted != currentRequested) {
                    // to make sure that we follow rule 1.8, 3.6 and 3.7
                    // We stop if cancellation was requested.
                    if (cancelled) {
                        return;
                    }

                    this.subscriber.onNext(index);

                    // Increment the index for the next possible emission.
                    index++;
                    // Increment the emitted count to prevent overflowing the downstream.
                    emitted++;
                }

                // If the index reached the end, we complete the downstream.
                if (index == end) {
                    // to make sure that we follow rule 1.8, 3.6 and 3.7
                    // Unless cancellation was requested by the last onNext.
                    if (!cancelled) {
                        // We need to consider this `Subscription` as cancelled as per rule 1.6
                        // Note, however, that this state is not observable from the outside
                        // world and since we leave the loop with requested > 0L, any
                        // further request() will never trigger the loop.
                        cancelled = true;

                        this.subscriber.onComplete();
                    }
                    return;
                }

                // Did the requested amount change while we were looping?
                long freshRequested = x.get();
                if (freshRequested == currentRequested) {
                    // Save where the loop has left off: the next value to be emitted
                    this.index = index;
                    // Atomically subtract the previously requested (also emitted) amount
                    currentRequested = x.addAndGet(-currentRequested);
                    // If there was no new request in between get() and addAndGet(), we simply quit
                    // The next 0 to N transition in request() will trigger the next emission loop.
                    if (currentRequested == 0L) {
                        break;
                    }
                    // Looks like there were more async requests, reset the emitted count and continue.
                    emitted = 0;
                } else {
                    // Yes, avoid the atomic subtraction and resume.
                    // emitted != currentRequest in this case and index
                    // still points to the next value to be emitted
                    currentRequested = freshRequested;
                }
            }
        }
    }
}
