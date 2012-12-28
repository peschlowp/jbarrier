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
 * Implementation of a dissemination barrier algorithm. The dissemination barrier was introduced in
 * the following article:
 * <p>
 * D. Hensgen, R. Finkel, and U. Manber. "Two Algorithms for Barrier Synchronization". In
 * <i>International Journal of Parallel Programming</i>, volume 17, pages 1-17, 1988.
 * <p>
 * The following two technical reports may server as good a introduction to different barrier
 * synchronization algorithms:
 * <ul>
 * <li>C. Ball and M. Bull. "Barrier Synchronisation in Java". 2003. (available online at
 * www.ukhec.ac.uk/publications/reports/synch_java.pdf at the time of writing)
 * <li>T. Hoefler, T. Mehlan, F. Mietke, and W. Rehm.
 * "A Survey of Barrier Algorithms for Coarse Grained Supercomputers". Technical University of
 * Chemnitz, 2004
 * </ul>
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class DisseminationBarrier extends AbstractBarrier {
    /**
     * The number of rounds used for the barrier.
     */
    protected final int numRounds;

    /**
     * The barrier information associated to each competitor thread.
     */
    protected DisseminationBarrierParty[] parties;

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
    public DisseminationBarrier(int numParties, Runnable barrierAction,
	    GenericReductor genericReductor) {
	super(numParties, barrierAction, genericReductor);
	if (!Utils.isPowerOfTwo(numParties)) {
	    throw new IllegalArgumentException(
		    "Dissemination barrier currently requires the number of parties to be a power of two!");
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
     * @param barrierAction
     *            the command to execute when the barrier is tripped, or <code>null</code> if there
     *            is no action
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public DisseminationBarrier(int numParties, Runnable barrierAction) {
	this(numParties, barrierAction, null);
    }

    /**
     * Constructor (if no action is used).
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public DisseminationBarrier(int numParties) {
	this(numParties, null);
    }

    /**
     * Sets up the parties array, intended to be overridden in subclasses.
     */
    protected void setUpParties() {
	parties = new DisseminationBarrierParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new DisseminationBarrierParty(i);
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
    protected class DisseminationBarrierParty {
	/**
	 * Unique id of this party [0 .. numParties-1].
	 */
	protected final int id;

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
	 * Array of incoming flags for this thread for each round. First key: parity (0 or 1).
	 * Second key: round number. (Note that a volatile boolean[][] is not enough here, because
	 * in that case updates would only be triggered when the array reference itself is changed
	 * but not when single array elements are modified.)
	 */
	protected final AtomicBoolean[][] flagsIn;

	/**
	 * Pre-computed array of references to my outgoing partners (whose flag I set) in every
	 * round.
	 */
	protected DisseminationBarrierParty[] partnersOut;

	/**
	 * Pre-computed array of thread IDs of my incoming partners (those that set my flag) in
	 * every round. Used for generic reduction only.
	 */
	private int[] partnersIn;

	/**
	 * Constructor.
	 * 
	 * @param threadId
	 *            the numeric id of this party within the barrier.
	 */
	protected DisseminationBarrierParty(int threadId) {
	    id = threadId;
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
	    partnersOut = new DisseminationBarrierParty[numRounds];
	    partnersIn = new int[numRounds];
	}

	/**
	 * Sets up this party for the barrier algorithm.
	 */
	protected void setupBarrier() {
	    for (int round = 0; round < numRounds; round++) {
		final int outPartner = (id + Utils.powerOfTwo(round)) % numParties;
		partnersOut[round] = parties[outPartner];
		int inPartner = (id - Utils.powerOfTwo(round)) % numParties;
		if (inPartner < 0) {
		    inPartner += numParties;
		}
		partnersIn[round] = inPartner;
	    }
	}

	/**
	 * Called when this party reaches the barrier.
	 */
	protected void await() {
	    for (int round = 0; round < numRounds; round++) {
		partnersOut[round].flagsIn[parity][round].set(sense);
		while (flagsIn[parity][round].get() != sense) {
		    // Busy-wait.
		}
		if (genericReductor != null) {
		    genericReductor.reduce(id, partnersIn[round]);
		}
	    }
	    if (parity == 1) {
		sense = !sense;
	    }
	    parity = 1 - parity;
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
