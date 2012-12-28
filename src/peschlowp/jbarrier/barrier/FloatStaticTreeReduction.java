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
 * Implementation of a static tree barrier algorithm including a float reduction.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class FloatStaticTreeReduction extends StaticTreeBarrier implements FloatReduction {
    /**
     * The reduction operator used.
     */
    private final ReductionOperator reductor;

    /**
     * The barrier data associated to each party.
     */
    private StaticTreeReductionParty[] parties;

    /**
     * Stores the result of the reduction.
     */
    private float result;

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
    public FloatStaticTreeReduction(int numParties, Runnable barrierAction,
	    ReductionOperator reductor) {
	super(numParties, barrierAction);
	this.reductor = reductor;
	setUpParties();
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
    public FloatStaticTreeReduction(int numParties, ReductionOperator reductor) {
	this(numParties, null, reductor);
    }

    /**
     * Sets up the parties array required for this reduction subclass.
     */
    @Override
    protected void setUpParties() {
	parties = new StaticTreeReductionParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new StaticTreeReductionParty(i);
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
    private class StaticTreeReductionParty extends StaticTreeBarrierParty {
	/**
	 * Intermediate value used during the reduction.
	 */
	private float value;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the numeric id of this thread within the barrier.
	 */
	private StaticTreeReductionParty(int id) {
	    super(id);
	}

	/**
	 * Called when this party reaches the barrier.
	 * 
	 * @param inValue
	 *            the value contributed to the reduction by this thread
	 * @return the result of the reduction
	 */
	private float await(float inValue) {
	    value = inValue;
	    sense = !sense;
	    if (id == 0) {
		while (flags[1].get() != sense) {
		    // Busy-wait.
		}
		value = reductor.operator(value, parties[1].value);
		if (numParties > 2) {
		    while (flags[2].get() != sense) {
			// Busy-wait.
		    }
		    value = reductor.operator(value, parties[2].value);
		    if (numParties > 3) {
			while (flags[numParties - 1].get() != sense) {
			    // Busy-wait.
			}
			result = reductor.operator(value, parties[numParties - 1].value);
		    }
		}
		// If there is a barrier action, execute it.
		if (action != null) {
		    action.run();
		}
		flagOut = sense;
	    } else if (id < THRESH) {
		while (flags[LEFT_CHILD].get() != sense) {
		    // Busy-wait.
		}
		value = reductor.operator(value, parties[LEFT_CHILD].value);
		while (flags[RIGHT_CHILD].get() != sense) {
		    // Busy-wait.
		}
		value = reductor.operator(value, parties[RIGHT_CHILD].value);
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
	    return result;
	}
    }
}
