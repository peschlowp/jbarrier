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

import java.util.concurrent.atomic.AtomicBoolean;

import peschlowp.jbarrier.util.Utils;

/**
 * Implementation of a butterfly barrier algorithm. The butterfly barrier was introduced in the
 * following article:
 * <p>
 * E. D. Brooks III. "The Butterfly Barrier". In <i>International Journal of Parallel
 * Programming</i>, volume 15, pages 295-307, 1986.
 * <p>
 * A good introduction to different barrier synchronization algorithms an be found in the following
 * technical report:
 * <p>
 * C. Ball and M. Bull. "Barrier Synchronisation in Java".
 * <p>
 * At the time of writing the report is available online at
 * www.ukhec.ac.uk/publications/reports/synch_java.pdf
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class ButterflyBarrier extends AbstractBarrier {
    /**
     * The number of rounds used for the barrier.
     */
    protected final int numRounds;

    /**
     * The barrier information associated to each competitor thread.
     */
    protected ButterflyBarrierParty[] parties;

    /**
     * Global out flag (only used if there is a barrier action).
     */
    protected volatile boolean flagOut;

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
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public ButterflyBarrier(int numParties, Runnable barrierAction, GenericReductor genericReductor) {
	super(numParties, barrierAction, genericReductor);
	if (!Utils.isPowerOfTwo(numParties)) {
	    throw new IllegalArgumentException(
		    "Butterfly barrier currently requires the number of parties to be a power of two!");
	}
	numRounds = (int) Math.ceil(Math.log(numParties) / Math.log(2.0));
	flagOut = false;
	setUpParties();
    }

    /**
     * Constructor.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @param action
     *            the command to execute when the barrier is tripped, or <code>null</code> if there
     *            is no action
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public ButterflyBarrier(int numParties, Runnable action) {
	this(numParties, action, null);
    }

    /**
     * Constructor (if no action is used).
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public ButterflyBarrier(int numParties) {
	this(numParties, null);
    }

    /**
     * Sets up the parties array, intended to be overridden in subclasses.
     */
    protected void setUpParties() {
	parties = new ButterflyBarrierParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new ButterflyBarrierParty(i);
	}
	for (int i = 0; i < numParties; i++) {
	    parties[i].setupBarrier();
	}
    }

    /**
     * Called by a party that reaches the barrier.
     * 
     * @param threadId
     *            the ID of the party
     */
    @Override
    public void await(int threadId) {
	parties[threadId].await();
    }

    /**
     * Stores data required by each party that uses the barrier.
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    protected class ButterflyBarrierParty {
	/**
	 * Unique id of this party [0 .. numParties-1].
	 */
	protected final int id;

	/**
	 * Array of incoming flags for this thread for each round. First key: parity (0 or 1).
	 * Second key: round number. (Note that a volatile boolean[][] is not enough here, because
	 * in that case updates would only be triggered when the array reference itself is changed
	 * but not when single array elements are modified.)
	 */
	protected final AtomicBoolean[][] flagsIn;

	/**
	 * Parity bit for alternating barrier episodes.
	 */
	protected int parity;

	/**
	 * Sense flag that switches between <code>true</code> and <code>false</code>.
	 */
	protected boolean sense;

	/**
	 * Sense flag for the global out flag (only used if there is a barrier action).
	 */
	protected boolean outSense;

	/**
	 * Pre-computed array of references to the partners of this party in each round.
	 */
	protected ButterflyBarrierParty[] partners;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the numeric id of this party within the barrier.
	 */
	protected ButterflyBarrierParty(int id) {
	    this.id = id;
	    parity = 0;
	    sense = false;
	    outSense = false;
	    flagsIn = new AtomicBoolean[2][numRounds];
	    for (int round = 0; round < numRounds; round++) {
		flagsIn[0][round] = new AtomicBoolean(!sense);
		flagsIn[1][round] = new AtomicBoolean(!sense);
	    }
	    setUpPartyData();
	}

	/**
	 * Sets up the partners array, intended to be overridden in subclasses.
	 */
	protected void setUpPartyData() {
	    partners = new ButterflyBarrierParty[numRounds];
	}

	/**
	 * Sets up this party for the barrier algorithm.
	 */
	protected void setupBarrier() {
	    for (int round = 0; round < numRounds; round++) {
		final int partner = (id ^ Utils.powerOfTwo(round)) % numParties;
		partners[round] = parties[partner];
	    }
	}

	/**
	 * Called when this party reaches the barrier.
	 */
	protected void await() {
	    for (int round = 0; round < numRounds; round++) {
		partners[round].flagsIn[parity][round].set(sense);
		while (flagsIn[parity][round].get() != sense) {
		    // Busy-wait.
		}
		if (genericReductor != null) {
		    genericReductor.reduce(id, partners[round].id);
		}
	    }
	    if (parity == 1) {
		sense = !sense;
	    }
	    parity = 1 - parity;
	    // If there is a barrier action, let thread 0 execute it.
	    if (action != null) {
		outSense = !outSense;
		if (id == 0) {
		    action.run();
		    flagOut = outSense;
		} else {
		    while (flagOut != outSense) {
			// Busy-wait.
		    }
		}
	    }
	}
    }
}
