/*
 * Copyright 2012 Patrick Peschlow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package peschlowp.jbarrier.barrier;

/**
 * Interface for all barrier implementations of this package. A barrier is a well-known
 * synchronization construct for two or more parties (here: threads). No party may pass the barrier
 * until all other parties have arrived at the barrier. Only then the next phase of parallel
 * computation is started.
 * 
 * A party reaches the barrier by calling the {@link #await(int)} method. There are two differences
 * between {@link #await(int)} and the corresponding method of the
 * {@link java.util.concurrent.CyclicBarrier}:
 * <ol>
 * <li>{@link #await(int)} has no return value.
 * <li>{@link #await(int)} takes as a parameter the logical ID of the thread calling the method.
 * <ol>
 * With respect to logical IDs, it is assumed that, if <i>n</i> threads take part in the barrier,
 * they have logical IDs ranging from 0..<i>n</i>-1. Each party has to know its ID and to specify it
 * when calling {@link #await(int)}. There are ways to implement the same behavior without having to
 * specify a logical ID, but we think our current implementation doesn't cause any inconvenience to
 * the user.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public interface Barrier {
    /**
     * Called by a party that reaches the barrier.
     * 
     * @param threadId
     *            the ID of the party (if <i>n</i> threads take part in the barrier, threadId must
     *            be one of 0..<i>n</i>-1)
     */
    public void await(int threadId);
}
