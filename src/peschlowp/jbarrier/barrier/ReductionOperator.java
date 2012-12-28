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
 * Abstract base class for reduction operators.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public abstract class ReductionOperator {
    /**
     * Operator on operands of type <code>int</code>.
     * 
     * @param value1
     *            the first operand
     * @param value2
     *            the second operand
     * @return the result of the operator applied to the two operands
     */
    protected abstract int operator(int value1, int value2);

    /**
     * Operator on operands of type <code>long</code>.
     * 
     * @param value1
     *            the first operand
     * @param value2
     *            the second operand
     * @return the result of the operator applied to the two operands
     */
    protected abstract long operator(long value1, long value2);

    /**
     * Operator on operands of type <code>float</code>.
     * 
     * @param value1
     *            the first operand
     * @param value2
     *            the second operand
     * @return the result of the operator applied to the two operands
     */
    protected abstract float operator(float value1, float value2);

    /**
     * Operator on operands of type <code>double</code>.
     * 
     * @param value1
     *            the first operand
     * @param value2
     *            the second operand
     * @return the result of the operator applied to the two operands
     */
    protected abstract double operator(double value1, double value2);

    /**
     * Minimum reduction operator.
     * 
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    public static class MinimumReduction extends ReductionOperator {
	/**
	 * Minimum operator on operands of type <code>int</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the minimum of the two operands
	 */
	@Override
	protected int operator(int value1, int value2) {
	    return value1 <= value2 ? value1 : value2;
	}

	/**
	 * Minimum operator on operands of type <code>long</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the minimum of the two operands
	 */
	@Override
	protected long operator(long value1, long value2) {
	    return value1 <= value2 ? value1 : value2;
	}

	/**
	 * Minimum operator on operands of type <code>float</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the minimum of the two operands
	 */
	@Override
	protected float operator(float value1, float value2) {
	    return value1 <= value2 ? value1 : value2;
	}

	/**
	 * Minimum operator on operands of type <code>double</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the minimum of the two operands
	 */
	@Override
	protected double operator(double value1, double value2) {
	    return value1 <= value2 ? value1 : value2;
	}
    }

    /**
     * Maximum reduction operator.
     * 
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    public static class MaximumReduction extends ReductionOperator {
	/**
	 * Maximum operator on operands of type <code>int</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the maximum of the two operands
	 */
	@Override
	protected int operator(int value1, int value2) {
	    return value1 >= value2 ? value1 : value2;
	}

	/**
	 * Maximum operator on operands of type <code>long</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the maximum of the two operands
	 */
	@Override
	protected long operator(long value1, long value2) {
	    return value1 >= value2 ? value1 : value2;
	}

	/**
	 * Maximum operator on operands of type <code>float</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the maximum of the two operands
	 */
	@Override
	protected float operator(float value1, float value2) {
	    return value1 >= value2 ? value1 : value2;
	}

	/**
	 * Maximum operator on operands of type <code>double</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the maximum of the two operands
	 */
	@Override
	protected double operator(double value1, double value2) {
	    return value1 >= value2 ? value1 : value2;
	}
    }

    /**
     * Sum reduction operator.
     * 
     * @version 1.0
     * 
     * @author Patrick Peschlow
     * @author Ivan Castilla Rodriguez
     */
    public static class SumReduction extends ReductionOperator {
	/**
	 * Sum operator on operands of type <code>int</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the sum of the two operands
	 */
	@Override
	protected int operator(int value1, int value2) {
	    return value1 + value2;
	}

	/**
	 * Sum operator on operands of type <code>long</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the sum of the two operands
	 */
	@Override
	protected long operator(long value1, long value2) {
	    return value1 + value2;
	}

	/**
	 * Sum operator on operands of type <code>float</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the sum of the two operands
	 */
	@Override
	protected float operator(float value1, float value2) {
	    return value1 + value2;
	}

	/**
	 * Sum operator on operands of type <code>double</code>.
	 * 
	 * @param value1
	 *            the first operand
	 * @param value2
	 *            the second operand
	 * @return the sum of the two operands
	 */
	@Override
	protected double operator(double value1, double value2) {
	    return value1 + value2;
	}
    }
}
