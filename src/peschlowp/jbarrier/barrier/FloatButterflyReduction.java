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

import peschlowp.jbarrier.util.Utils;

/**
 * Implementation of a butterfly barrier algorithm including a float reduction.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class FloatButterflyReduction extends ButterflyBarrier implements FloatReduction {
    /**
     * The reduction operator used.
     */
    private final ReductionOperator reductor;

    /**
     * The barrier data associated to each party.
     */
    private ButterflyReductionParty[] parties;

    /**
     * Constructor.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @param barrierAction
     *            the command to execute when the barrier is tripped, or <code>null</code> if there
     *            is no action
     * @param reductor
     *            the reduction operator to use
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public FloatButterflyReduction(int numParties, Runnable barrierAction,
	    ReductionOperator reductor) {
	super(numParties, barrierAction);
	this.reductor = reductor;
    }

    /**
     * Constructor.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @param reductor
     *            the reduction operator to use
     * @throws IllegalArgumentException
     *             if <code>numParties</code> is not a power of two
     */
    public FloatButterflyReduction(int numParties, ReductionOperator reductor) {
	this(numParties, null, reductor);
    }

    /**
     * Sets up the parties array required for this reduction subclass.
     */
    @Override
    protected void setUpParties() {
	parties = new ButterflyReductionParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new ButterflyReductionParty(i);
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
     * @param value
     *            the value subject to the reduction
     * @return the result of the reduction
     */
    @Override
    public float await(int threadId, float value) {
	return parties[threadId].await(value);
    }

    /**
     * Stores data required by each party that uses the barrier.
     * 
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    private class ButterflyReductionParty extends ButterflyBarrierParty {
	/**
	 * Pre-computed array of references to the partners of this party in each round.
	 */
	private ButterflyReductionParty[] partners;

	/**
	 * Intermediate values used during the reduction. First key: parity (0 or 1). Second key:
	 * round number.
	 */
	private final float[][] values;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the numeric id of this party within the barrier
	 */
	private ButterflyReductionParty(int id) {
	    super(id);
	    values = new float[2][numRounds + 1];
	}

	/**
	 * Sets up the partners array for this reduction subclass.
	 */
	@Override
	protected void setUpPartyData() {
	    partners = new ButterflyReductionParty[numRounds];
	}

	/**
	 * Sets up the partners array for this reduction subclass.
	 */
	@Override
	protected void setupBarrier() {
	    for (int round = 0; round < numRounds; round++) {
		final int partner = (id ^ Utils.powerOfTwo(round)) % numParties;
		partners[round] = parties[partner];
	    }
	}

	/**
	 * Called when this party reaches the barrier.
	 * 
	 * @param inValue
	 *            the value contributed to the reduction by this thread
	 * @return the result of the reduction
	 */
	private float await(float inValue) {
	    values[parity][0] = inValue;
	    for (int round = 0; round < numRounds; round++) {
		partners[round].flagsIn[parity][round].set(sense);
		while (flagsIn[parity][round].get() != sense) {
		    // Busy-wait.
		}
		values[parity][round + 1] = reductor.operator(values[parity][round],
			partners[round].values[parity][round]);
	    }
	    final float result = values[parity][numRounds];
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
	    return result;
	}
    }
}
