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
 * Implementation of a static tree barrier algorithm. The static tree barrier was introduced in the
 * following article:
 * <p>
 * J. M. Mellor-Crummey and M. L. Scott. "Algorithms for Scalable Synchronization on Shared-Memory
 * Multiprocessors". In <i>ACM Transactions on Computer Systems</i>, volume 9, pages 21-65, 1991.
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
public class StaticTreeBarrier extends AbstractBarrier {
    /**
     * The barrier data associated to each party.
     */
    protected StaticTreeBarrierParty[] parties;

    /**
     * The synchronization flags (one for each party).
     */
    protected final AtomicBoolean[] flags;

    /**
     * Out flag set by the winner.
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
    public StaticTreeBarrier(int numParties, Runnable barrierAction, GenericReductor genericReductor) {
	super(numParties, barrierAction, genericReductor);
	if (!Utils.isPowerOfTwo(numParties)) {
	    throw new IllegalArgumentException(
		    "Static tree barrier currently requires the number of parties to be a power of two!");
	}
	flagOut = false;
	flags = new AtomicBoolean[numParties];
	for (int i = 0; i < numParties; i++) {
	    flags[i] = new AtomicBoolean(false);
	}
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
    public StaticTreeBarrier(int numParties, Runnable barrierAction) {
	this(numParties, barrierAction, null);
    }

    /**
     * Constructor.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public StaticTreeBarrier(int numParties) {
	this(numParties, null);
    }

    /**
     * Sets up the parties array, intended to be overridden in subclasses.
     */
    protected void setUpParties() {
	parties = new StaticTreeBarrierParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new StaticTreeBarrierParty(i);
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
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    protected class StaticTreeBarrierParty {
	/**
	 * Unique id of this party [0 .. numParties-1].
	 */
	protected final int id;

	/**
	 * Sense flag that switches between <code>true</code> and <code>false</code>.
	 */
	protected boolean sense;

	/**
	 * Pre-computed constant that indicates the first leaf node.
	 */
	protected final int THRESH;

	/**
	 * Pre-computed constant that represents the ID of my left child in the tree.
	 */
	protected final int LEFT_CHILD;

	/**
	 * Pre-computed constant that represents the ID of my right child in the tree.
	 */
	protected final int RIGHT_CHILD;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the numeric id of this thread within the barrier.
	 */
	protected StaticTreeBarrierParty(int id) {
	    this.id = id;
	    sense = false;
	    THRESH = (numParties - 1) / 2;
	    LEFT_CHILD = 2 * id + 1;
	    RIGHT_CHILD = 2 * id + 2;
	}

	/**
	 * Called when this party reaches the barrier.
	 */
	protected void await() {
	    sense = !sense;
	    if (id == 0) {
		while (flags[1].get() != sense) {
		    // Busy-wait.
		}
		if (genericReductor != null) {
		    genericReductor.reduce(id, 1);
		}
		if (numParties > 2) {
		    while (flags[2].get() != sense) {
			// Busy-wait.
		    }
		    if (genericReductor != null) {
			genericReductor.reduce(id, 2);
		    }
		    if (numParties > 3) {
			while (flags[numParties - 1].get() != sense) {
			    // Busy-wait.
			}
			if (genericReductor != null) {
			    genericReductor.reduce(id, numParties - 1);
			}
		    }
		}
		if (action != null) {
		    action.run();
		}
		flagOut = sense;
	    } else if (id < THRESH) {
		while (flags[LEFT_CHILD].get() != sense) {
		    // Busy-wait.
		}
		if (genericReductor != null) {
		    genericReductor.reduce(id, LEFT_CHILD);
		}
		while (flags[RIGHT_CHILD].get() != sense) {
		    // Busy-wait.
		}
		if (genericReductor != null) {
		    genericReductor.reduce(id, RIGHT_CHILD);
		}
		flags[id].set(sense);
		while (flagOut != sense) {
		    // Busy-wait.
		}
	    } else {
		flags[id].set(sense);
		while (flagOut != sense) {
		    // Busy-wait.
		}
	    }
	}
    }
}
