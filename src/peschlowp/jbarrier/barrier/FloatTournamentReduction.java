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
 * Implementation of a tournament barrier algorithm including a float reduction.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class FloatTournamentReduction extends TournamentBarrier implements FloatReduction {
    /**
     * The reduction operator used.
     */
    private final ReductionOperator reductor;

    /**
     * The barrier data associated to each party.
     */
    private TournamentReductionParty[] parties;

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
    public FloatTournamentReduction(int numParties, Runnable barrierAction,
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
     */
    public FloatTournamentReduction(int numParties, ReductionOperator reductor) {
	this(numParties, null, reductor);
    }

    /**
     * Sets up the parties array required for this reduction subclass.
     */
    @Override
    protected void setUpParties() {
	parties = new TournamentReductionParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new TournamentReductionParty(i);
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
    private class TournamentReductionParty extends TournamentBarrierParty {
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
	private TournamentReductionParty(int id) {
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
	    int currentRound = 0;
	    for (;;) {
		final Round roundObj = rounds[currentRound];
		switch (roundObj.role) {
		case WINNER:
		    while (flags[currentRound].get() != sense) {
			// Busy-wait.
		    }
		    value = reductor.operator(value, parties[roundObj.partnerId].value);
		    ++currentRound;
		    // Continue to next round.
		    continue;
		case WILDCARD:
		    ++currentRound;
		    // Continue to next round.
		    continue;
		case LOSER:
		    parties[roundObj.partnerId].flags[currentRound].set(sense);
		    // Wait for the tournament winner (root).
		    while (flagOut != sense) {
			// Busy-wait.
		    }
		    // Exit switch statement (and thus the for loop).
		    break;
		case ROOT:
		    while (flags[currentRound].get() != sense) {
			// Busy-wait.
		    }
		    result = reductor.operator(value, parties[roundObj.partnerId].value);
		    // If there is a barrier action, execute it.
		    if (action != null) {
			action.run();
		    }
		    flagOut = sense;
		    // Exit switch statement (and thus the for loop).
		    break;
		}
		// Exit for loop.
		break;
	    }
	    return result;
	}
    }
}
