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
 * Implementation of a tournament barrier algorithm. A tournament barrier algorithm, which makes the
 * parties compete in a series of game "rounds", arranged in a tournament structure. The winning
 * party advances to the next level and "plays" against other winning parties until there is only a
 * single "champion" left. The loser parties simply wait for the tournament to finish (when they are
 * finally woken up by the champion). Since this is not a real competition, the winners and losers
 * of each round are selected in advance to improve performance. The tournament barrier was
 * introduced in the following article:
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
public class TournamentBarrier extends AbstractBarrier {
    /**
     * The barrier information associated to each competitor thread.
     */
    protected TournamentBarrierParty[] parties;

    /**
     * The number of rounds used for the barrier.
     */
    protected final int numRounds;

    /**
     * Out flag set by the winner of the tournament.
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
     */
    public TournamentBarrier(int numParties, Runnable barrierAction, GenericReductor genericReductor) {
	super(numParties, barrierAction, genericReductor);
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
     */
    public TournamentBarrier(int numParties, Runnable barrierAction) {
	this(numParties, barrierAction, null);
    }

    /**
     * Constructor (if no action is used).
     * 
     * @param numParties
     *            the number of parties that must reach the barrier before the barrier is tripped
     */
    public TournamentBarrier(int numParties) {
	this(numParties, null);
    }

    /**
     * Sets up the parties array, intended to be overridden in subclasses.
     */
    protected void setUpParties() {
	parties = new TournamentBarrierParty[numParties];
	for (int i = 0; i < numParties; i++) {
	    parties[i] = new TournamentBarrierParty(i);
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
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    protected class TournamentBarrierParty {
	/**
	 * Unique id of this party [0 .. numParties-1].
	 */
	protected final int id;

	/**
	 * Sense flag that switches between <code>true</code> and <code>false</code>.
	 */
	protected boolean sense;

	/**
	 * Pre-computed information about the rounds of the barrier.
	 */
	protected final Round[] rounds;

	/**
	 * My set of flags to be set/queried during the barrier.
	 */
	protected final AtomicBoolean[] flags;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *            the numeric id of this thread within the barrier.
	 */
	protected TournamentBarrierParty(int id) {
	    this.id = id;
	    sense = false;
	    flags = new AtomicBoolean[numRounds];
	    rounds = new Round[numRounds];
	}

	/**
	 * Sets up this party for the barrier algorithm.
	 */
	protected void setupBarrier() {
	    // Compute the next higher power of two to build the tree.
	    final int numVirtualThreads = Utils.nextHigherPowerOfTwo(numParties);
	    for (int round = 0; round < numRounds; round++) {
		int partnerId = (id ^ Utils.powerOfTwo(round)) % numVirtualThreads;
		final boolean isWinner = (id % Utils.powerOfTwo(round + 1) == 0);
		Role role;
		if (partnerId >= numParties) {
		    role = Role.WILDCARD;
		    partnerId = -1;
		} else {
		    if (isWinner) {
			if (id == 0 && round == numRounds - 1) {
			    role = Role.ROOT;
			} else {
			    role = Role.WINNER;
			}
		    } else {
			role = Role.LOSER;
		    }
		}
		Round roundObj = new Round(partnerId, role);
		rounds[round] = roundObj;
		flags[round] = new AtomicBoolean(false);
	    }
	}

	/**
	 * Called when this party reaches the barrier.
	 */
	protected void await() {
	    sense = !sense;
	    int currentRound = 0;
	    for (;;) {
		final Round roundObj = rounds[currentRound];
		switch (roundObj.role) {
		case WINNER:
		    while (flags[currentRound].get() != sense) {
			// Busy-wait.
		    }
		    if (genericReductor != null) {
			genericReductor.reduce(id, roundObj.partnerId);
		    }
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
		    if (genericReductor != null) {
			genericReductor.reduce(id, roundObj.partnerId);
		    }
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
	}
    }

    /**
     * Possible roles of the different parties in the tournament barrier.
     * 
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    protected static enum Role {
	/**
	 * Winner. Advances to the next round.
	 */
	WINNER,
	/**
	 * Loser. Out of the tournament.
	 */
	LOSER,
	/**
	 * Wildcard. Advances to the next round without having a partner.
	 */
	WILDCARD,
	/**
	 * Root. Overall winner of the tournament.
	 */
	ROOT;
    }

    /**
     * Stores pre-computed information about a round of the tournament barrier.
     * 
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    protected static class Round {
	/**
	 * ID of my "competitor" party in this round.
	 */
	protected final int partnerId;

	/**
	 * My role in this round.
	 */
	protected final Role role;

	/**
	 * Constructor.
	 * 
	 * @param partnerId
	 *            the ID of the "competitor" party in the current round
	 * @param role
	 *            the role of this party in the current round
	 */
	private Round(int partnerId, Role role) {
	    this.partnerId = partnerId;
	    this.role = role;
	}
    }
}
