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
 * Implementation of a central barrier algorithm including a float reduction.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class FloatCentralReduction extends CentralBarrier implements FloatReduction {
    /**
     * The reduction operator used.
     */
    private final ReductionOperator reductor;

    /**
     * The barrier data associated to each party.
     */
    private final CentralReductionParty[] parties;

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
     */
    public FloatCentralReduction(int numParties, Runnable barrierAction, ReductionOperator reductor) {
	super(numParties, barrierAction);
	this.reductor = reductor;
	parties = new CentralReductionParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new CentralReductionParty(i);
	}
	counter = new AtomicInteger(0);
	go = false;
    }

    /**
     * Constructor.
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     * @param reductor
     *            the reduction operator to use
     */
    public FloatCentralReduction(int numParties, ReductionOperator reductor) {
	this(numParties, null, reductor);
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
    private class CentralReductionParty {
	/**
	 * Unique id of this party [0 .. numParties-1].
	 */
	@SuppressWarnings("unused")
	private final int id;

	/**
	 * Intermediate value used during the reduction.
	 */
	private float value;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the numeric id of this party within the barrier.
	 */
	private CentralReductionParty(int id) {
	    this.id = id;
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
	    boolean localGo = go;
	    if (counter.incrementAndGet() == numParties) {
		counter.set(0);
		float tmpResult = parties[0].value;
		for (int i = 1; i < numParties; i++) {
		    tmpResult = reductor.operator(tmpResult, parties[i].value);
		}
		result = tmpResult;
		// If there is a barrier action, execute it.
		if (action != null) {
		    action.run();
		}
		go = !go;
	    } else {
		while (go == localGo) {
		    // Busy-wait.
		}
	    }
	    return result;
	}
    }
}
