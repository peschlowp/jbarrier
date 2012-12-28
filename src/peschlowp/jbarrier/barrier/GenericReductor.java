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
 * Interface for generic reductors. A call to {@link #reduce(int, int)} is meant to perform an
 * arbitrary binary reduction task between two parties. In order to use a generic reductor, it is
 * required to let the threads store the intermediate results of each binary reduction as well as
 * the final result of the global reduction.
 * 
 * @version 1.0
 * 
 * @author Patrick Peschlow
 * @author Ivan Castilla Rodriguez
 */
public interface GenericReductor {
    /**
     * Performs a binary reduction for two threads participating in the barrier. The barrier
     * implementations make the assumption that the first thread receives the result of the
     * reduction, i.e., the first (= the calling) thread does not affect any local variable of the
     * second thread.
     * 
     * @param threadId1
     *            the ID of the first thread involved in the reduction
     * @param threadId2
     *            the ID of the second thread involved in the reduction
     */
    void reduce(int threadId1, int threadId2);
}
