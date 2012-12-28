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
 * Abstract base class for all barrier implementations of this package. Barriers extending this
 * class can have an associated (global) action, specified as a {@link Runnable}, which will be
 * executed by one party when all parties have reached the barrier. Also, arbitrary global
 * reductions are supported with the help of a {@link GenericReductor} object.
 * <p>
 * With respect to memory consistency, just like with the {@link java.util.concurrent.CyclicBarrier}
 * , actions in a thread prior to calling {@link #await(int)} happen-before actions that are part of
 * the barrier action, which in turn happen-before actions following a successful return from
 * {@link #await(int)} in other threads.
 * <p>
 * At this point, there is not much of an error handling if, e.g., threads get interrupted during
 * the barrier. A well-defined error handling, such as the
 * {@link java.util.concurrent.BrokenBarrierException} used by the
 * {@link java.util.concurrent.CyclicBarrier}, may be added in future versions.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public abstract class AbstractBarrier implements Barrier {
    /**
     * The number of parties taking part in the barrier.
     */
    protected final int numParties;

    /**
     * The command to execute when the barrier is tripped, or <code>null</code> if there is no
     * action.
     */
    protected final Runnable action;

    /**
     * An optional generic reduction operator.
     */
    protected final GenericReductor genericReductor;

    /**
     * Creates a new <code>AbstractBarrier</code> that will trip when the given number of parties
     * are waiting upon it.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @param action
     *            the command to execute when the barrier is tripped, or <code>null</code> if there
     *            is no action
     * @param genericReductor
     *            an optional generic reductor
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is less than 2
     */
    protected AbstractBarrier(int numParties, Runnable action, GenericReductor genericReductor) {
	if (numParties < 2) {
	    throw new IllegalArgumentException("Number of parties has to be larger than one!");
	}
	this.numParties = numParties;
	this.action = action;
	this.genericReductor = genericReductor;
    }

    /**
     * Called by a party that reaches the barrier.
     * 
     * @param threadId
     *            the ID of the party
     */
    @Override
    public abstract void await(int threadId);
}
