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
package peschlowp.jbarrier.util;

/**
 * Utility functions for computing powers of two.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public class Utils {

    /**
     * Computes the next higher power of two of the provided integer.
     * 
     * @param j
     *            the provided integer
     * @return the next higher power of two of <code>j</code>
     */
    public static int nextHigherPowerOfTwo(int j) {
	int k = j - 1;
	for (int i = 1; i < 32; i <<= 1) {
	    k = k | k >> i;
	}
	return k + 1;
    }

    /**
     * Computes the <code>n</code>-th integer power of two.
     * 
     * @param n
     *            the provided integer
     * @return the <code>n</code>-th power of two
     */
    public static int powerOfTwo(int n) {
	return 1 << n;
    }

    /**
     * Checks whether the provided integer is a power of two.
     * 
     * @param k
     *            the provided integer
     * @return <code>true</code> if the provided integer is a power of two, otherwise
     *         <code>false</code>
     */
    public static boolean isPowerOfTwo(int k) {
	return (k & (k - 1)) == 0;
    }
}
