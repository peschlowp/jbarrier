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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of a central barrier algorithm based on a shared counter. Of all algorithms in
 * this package, this one is most similar to the {@link java.util.concurrent.CyclicBarrier}.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class CentralBarrier extends AbstractBarrier {
    /**
     * The central counter variable.
     */
    protected AtomicInteger counter;

    /**
     * Global out flag.
     */
    protected volatile boolean go;

    /**
     * Constructor.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @param barrierAction
     *            the command to execute when the barrier is tripped, or <code>null</code> if there
     *            is no action
     * @param genericReductor
     *            an optional generic reductor
     */
    public CentralBarrier(int numParties, Runnable barrierAction, GenericReductor genericReductor) {
	super(numParties, barrierAction, genericReductor);
	counter = new AtomicInteger(0);
	go = false;
    }

    /**
     * Constructor.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @param barrierAction
     *            the command to execute when the barrier is tripped, or <code>null</code> if there
     *            is no action
     */
    public CentralBarrier(int numParties, Runnable barrierAction) {
	this(numParties, barrierAction, null);
    }

    /**
     * Constructor (if no action is used).
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     */
    public CentralBarrier(int numParties) {
	this(numParties, null);
    }

    /**
     * Called by a party that reaches the barrier.
     * 
     * @param threadId
     *            the ID of the party
     */
    @Override
    public void await(int threadId) {
	boolean localGo = go;
	if (counter.incrementAndGet() == numParties) {
	    counter.set(0);
	    if (genericReductor != null) {
		for (int i = 1; i < numParties; i++) {
		    genericReductor.reduce(0, i);
		}
	    }
	    if (action != null) {
		action.run();
	    }
	    go = !go;
	} else {
	    while (go == localGo) {
		// Busy-wait.
	    }
	}
    }
}
