/***************************************************
 * Licensed under MIT No Attribution (SPDX: MIT-0) *
 ***************************************************/

package _org.reactivestreams.example.unicast.publisher;

import java.util.Iterator;
import java.util.concurrent.Executor;

public class InfiniteIncrementNumberPublisher extends AsyncIterablePublisher<Integer> {
    public InfiniteIncrementNumberPublisher(final Executor executor) {
        super(new Iterable<Integer>() {
          @Override public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
              private int at = 0;
              @Override public boolean hasNext() { return true; }
              @Override public Integer next() { return at++; } // Wraps around on overflow
              @Override public void remove() { throw new UnsupportedOperationException(); }
            };
          }
        }, executor);
    }
}